package io.onedev.server.search.entity.pullrequest;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import io.onedev.server.model.PullRequest;
import io.onedev.server.model.User;
import io.onedev.server.util.ProjectScope;
import io.onedev.server.util.criteria.AndCriteria;
import io.onedev.server.util.criteria.Criteria;

public class ToBeMergedByCriteria extends Criteria<PullRequest> {

	private static final long serialVersionUID = 1L;

	private final User user;
	
	public ToBeMergedByCriteria(User user) {
		this.user = user;
	}
	
	@Override
	public Predicate getPredicate(@Nullable ProjectScope projectScope, CriteriaQuery<?> query, From<PullRequest, PullRequest> from, CriteriaBuilder builder) {
		return getCriteria(user).getPredicate(projectScope, query, from, builder);
	}

	@Override
	public boolean matches(PullRequest request) {
		return getCriteria(user).matches(request);
	}

	@SuppressWarnings("unchecked")
	private Criteria<PullRequest> getCriteria(User user) {
		return new AndCriteria<>(new ReadyToMergeCriteria(), new AssignedToCriteria(user));
	}
	
	@Override
	public String toStringWithoutParens() {
		return PullRequestQuery.getRuleName(PullRequestQueryLexer.ToBeMergedBy) + " " + quote(user.getName());
	}

}
