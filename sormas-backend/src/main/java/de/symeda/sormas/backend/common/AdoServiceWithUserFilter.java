package de.symeda.sormas.backend.common;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.symeda.sormas.api.RequestContextHolder;
import org.apache.commons.lang3.StringUtils;

import de.symeda.sormas.backend.user.User;

public abstract class AdoServiceWithUserFilter<ADO extends AbstractDomainObject> extends BaseAdoService<ADO> {

	public static final int NR_OF_LAST_PHONE_DIGITS_TO_SEARCH = 6;

	public AdoServiceWithUserFilter(Class<ADO> elementClass) {
		super(elementClass);
	}

	/**
	 * Used by most getAll* and getAllUuids methods to filter by user
	 */
	@SuppressWarnings("rawtypes")
	public abstract Predicate createUserFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, ADO> from);

	protected Predicate limitSynchronizationFilter(CriteriaBuilder cb, From<?, ADO> from) {
		return null;
	}

	protected Predicate limitSynchronizationFilterObsoleteEntities(CriteriaBuilder cb, From<?, ADO> from) {
		return null;
	}

	/**
	 * Default filter for {@link #getAllAfter(Date)} to data that is allowed for the user to access and relevant for sync.
	 * If you override this method it should usually include {@link #createUserFilter(CriteriaBuilder, CriteriaQuery, From)}
	 * within the filter.
	 */
	@SuppressWarnings("rawtypes")
	protected Predicate createRelevantDataFilter(CriteriaBuilder cb, CriteriaQuery cq, From<?, ADO> from) {
		return createUserFilter(cb, cq, from);
	}

	public List<ADO> getAllAfter(Date since) {
		return getAllAfter(since, null, null);
	}

	public List<ADO> getAllAfter(Date since, Integer batchSize, String lastSynchronizedUuid) {

		return getList((cb, cq, from) -> {

			Predicate filter = createRelevantDataFilter(cb, cq, from);
			if (since != null) {
				filter = CriteriaBuilderHelper.and(cb, filter, createChangeDateFilter(cb, from, since, lastSynchronizedUuid));
			}

			Predicate predicate = limitSynchronizationFilter(cb, from);
			if (predicate != null) {
				filter = CriteriaBuilderHelper.and(cb, predicate);
			}

			return filter;
		}, batchSize);
	}

	public List<String> getAllUuids() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		Root<ADO> from = cq.from(getElementClass());

		Predicate filter = createUserFilter(cb, cq, from);
		Predicate predicate = limitSynchronizationFilter(cb, from);
		if (predicate != null) {
			filter = CriteriaBuilderHelper.and(cb, predicate);
		}

		if (filter != null) {
			cq.where(filter);
		}

		cq.select(from.get(AbstractDomainObject.UUID));
		return em.createQuery(cq).getResultList();
	}

	public List<Long> getAllIds(User user) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<ADO> from = cq.from(getElementClass());

		if (user != null) {
			Predicate filter = createUserFilter(cb, cq, from);
			Predicate predicate = limitSynchronizationFilter(cb, from);
			if (predicate != null) {
				filter = CriteriaBuilderHelper.and(cb, predicate);
			}

			if (filter != null) {
				cq.where(filter);
			}
		}

		cq.select(from.get(AbstractDomainObject.ID));
		return em.createQuery(cq).getResultList();
	}

	public List<String> getObsoleteUuidsSince(Date since) {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<String> cq = cb.createQuery(String.class);
		Root<ADO> from = cq.from(getElementClass());

		Predicate filter = getUserFilterForObsoleteUuids(cb, cq, from);
		if (since != null) {
			filter = CriteriaBuilderHelper.and(cb, filter, cb.greaterThanOrEqualTo(from.get(AbstractDomainObject.CHANGE_DATE), since));
		}

		Predicate contentFilter = null;

		if (DeletableAdo.class.isAssignableFrom(getElementClass())) {
			contentFilter = CriteriaBuilderHelper.or(cb, contentFilter, cb.equal(from.get(DeletableAdo.DELETED), true));
		}

		if (CoreAdo.class.isAssignableFrom(getElementClass())) {
			contentFilter = CriteriaBuilderHelper.or(cb, contentFilter, cb.equal(from.get(CoreAdo.ARCHIVED), true));
		}

		for (Predicate additionalPredicate : getAdditionalObsoleteUuidsPredicates(since, cb, cq, from)) {
			contentFilter = CriteriaBuilderHelper.or(cb, contentFilter, additionalPredicate);
		}

		filter = CriteriaBuilderHelper.and(cb, filter, contentFilter);

		if (RequestContextHolder.isMobileSync()) {
			Predicate predicate = limitSynchronizationFilterObsoleteEntities(cb, from);
			if (predicate != null) {
				filter = CriteriaBuilderHelper.and(cb, predicate);
			}
		}

		cq.where(filter);
		cq.select(from.get(AbstractDomainObject.UUID));

		return em.createQuery(cq).getResultList();
	}

	protected Predicate getUserFilterForObsoleteUuids(CriteriaBuilder cb, CriteriaQuery<String> cq, Root<ADO> from) {
		return createUserFilter(cb, cq, from);
	}

	protected List<Predicate> getAdditionalObsoleteUuidsPredicates(Date since, CriteriaBuilder cb, CriteriaQuery<String> cq, Root<ADO> from) {
		return Collections.emptyList();
	}

	protected String formatForLike(String textFilter) {
		return "%" + textFilter.toLowerCase() + "%";
	}

	protected Predicate phoneNumberPredicate(CriteriaBuilder cb, Expression<String> path, String textFilter) {
		return cb.like(removeNonNumbersExpression(cb, path), formatPhoneNumberForSearch(textFilter));
	}

	protected String formatPhoneNumberForSearch(String textFilter) {
		final String formattedPhoneNumber = textFilter.replaceAll("[^0-9a-zA-Z]", "");
		if (StringUtils.isEmpty(formattedPhoneNumber)) {
			return textFilter;
		}
		final int phoneNrLength = formattedPhoneNumber.length();
		return formatForLike(
			phoneNrLength >= NR_OF_LAST_PHONE_DIGITS_TO_SEARCH
				? formattedPhoneNumber.substring(phoneNrLength - NR_OF_LAST_PHONE_DIGITS_TO_SEARCH, phoneNrLength)
				: formattedPhoneNumber);
	}

	protected Expression<String> removeNonNumbersExpression(CriteriaBuilder cb, Expression<String> path) {
		return cb.function("REGEXP_REPLACE", String.class, path, cb.literal("[^0-9]"), cb.literal(""), cb.literal("g"));
	}
}
