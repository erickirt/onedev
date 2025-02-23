package io.onedev.server.search.entity.pack;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import io.onedev.commons.utils.StringUtils;
import io.onedev.server.model.Pack;
import io.onedev.server.util.ProjectScope;
import io.onedev.server.util.criteria.Criteria;
import io.onedev.server.util.criteria.OrCriteria;

public class FuzzyCriteria extends Criteria<Pack> {

	private static final long serialVersionUID = 1L;

	private final String value;
	
	public FuzzyCriteria(String value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(@Nullable ProjectScope projectScope, CriteriaQuery<?> query, From<Pack, Pack> from, CriteriaBuilder builder) {
		return parse(value).getPredicate(projectScope, query, from, builder);
	}

	@Override
	public boolean matches(Pack build) {
		return parse(value).matches(build);
	}
	
	@SuppressWarnings("unchecked")
	private Criteria<Pack> parse(String value) {
		return new OrCriteria<>(
				new NameCriteria("*" + value + "*", PackQueryLexer.Is),
				new VersionCriteria("*" + value + "*", PackQueryLexer.Is));
	}

	@Override
	public String toStringWithoutParens() {
		return "~" + StringUtils.escape(value, "~") + "~";
	}

}
