package io.onedev.server.model;

import static io.onedev.server.model.CodeComment.PROP_CREATE_DATE;
import static io.onedev.server.search.entity.EntitySort.Direction.DESCENDING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.onedev.server.OneDev;
import io.onedev.server.attachment.AttachmentStorageSupport;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.git.service.GitService;
import io.onedev.server.model.support.CompareContext;
import io.onedev.server.model.support.LastActivity;
import io.onedev.server.model.support.Mark;
import io.onedev.server.model.support.ProjectBelonging;
import io.onedev.server.search.entity.SortField;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.xodus.VisitInfoManager;

@Entity
@Table(indexes={
		@Index(columnList="o_project_id"), @Index(columnList="o_user_id"),
		@Index(columnList="o_pullRequest_id"),
		@Index(columnList=Mark.PROP_COMMIT_HASH), @Index(columnList=Mark.PROP_PATH), 
		@Index(columnList=PROP_CREATE_DATE), @Index(columnList= LastActivity.COLUMN_DATE)})
public class CodeComment extends ProjectBelonging implements AttachmentStorageSupport {
	
	private static final long serialVersionUID = 1L;
	
	public static final int MAX_CONTENT_LEN = 100000;
	
	public static final String PROP_PROJECT = "project";
	
	public static final String PROP_COMPARE_CONTEXT = "compareContext";
	
	public static final String NAME_CONTENT = "Content";
	
	public static final String PROP_CONTENT = "content";
	
	public static final String NAME_REPLY = "Reply";
	
	public static final String NAME_PATH = "Path";
	
	public static final String PROP_MARK = "mark";
	
	public static final String NAME_REPLY_COUNT = "Reply Count";
	
	public static final String PROP_REPLY_COUNT = "replyCount";
	
	public static final String NAME_CREATE_DATE = "Create Date";
	
	public static final String PROP_CREATE_DATE = "createDate";
	
	public static final String NAME_LAST_ACTIVITY_DATE = "Last Activity Date";
	
	public static final String PROP_LAST_ACTIVITY = "lastActivity";
	
	public static final String NAME_RESOLVED = "Status";
	
	public static final String PROP_RESOLVED = "resolved";
	
	public static final String PROP_USER = "user";
	
	public static final String PROP_UUID = "uuid";

	public static final List<String> QUERY_FIELDS = Lists.newArrayList(
			NAME_CONTENT, NAME_REPLY, NAME_PATH, NAME_CREATE_DATE, NAME_LAST_ACTIVITY_DATE, NAME_REPLY_COUNT);

	public static final Map<String, SortField<CodeComment>> SORT_FIELDS = new LinkedHashMap<>();
	static {
		SORT_FIELDS.put(NAME_RESOLVED, new SortField<>(PROP_RESOLVED));
		SORT_FIELDS.put(NAME_CREATE_DATE, new SortField<>(PROP_CREATE_DATE, DESCENDING));
		SORT_FIELDS.put(NAME_LAST_ACTIVITY_DATE, new SortField<>(PROP_LAST_ACTIVITY + "." + LastActivity.PROP_DATE, DESCENDING));
		SORT_FIELDS.put(NAME_REPLY_COUNT, new SortField<>(PROP_REPLY_COUNT, DESCENDING));
	}

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private Project project;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(nullable=false)
	private User user;
	
	@Column(nullable=false, length=MAX_CONTENT_LEN)
	private String content;
	
	@Column(nullable=false)
	private Date createDate = new Date();

	@Embedded
	private LastActivity lastActivity;
	
	private int replyCount;

	@Embedded
	private Mark mark;
	
	@Embedded
	private CompareContext compareContext;
	
	private boolean resolved;

	@OneToMany(mappedBy="comment", cascade=CascadeType.REMOVE)
	private Collection<CodeCommentMention> mentions = new ArrayList<>();
	
	@OneToMany(mappedBy="comment", cascade=CascadeType.REMOVE)
	private Collection<CodeCommentReply> replies = new ArrayList<>();
	
	@OneToMany(mappedBy="comment", cascade=CascadeType.REMOVE)
	private Collection<CodeCommentStatusChange> changes = new ArrayList<>();
	
	@OneToMany(mappedBy="comment", cascade=CascadeType.REMOVE)
	private Collection<PendingSuggestionApply> pendingSuggestionApplies = new ArrayList<>();
	
	@Column(nullable=false)
	private String uuid = UUID.randomUUID().toString();
	
	private transient Collection<User> participants;
	
	@Override
	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public User getUser() {
		return user;
	}

	public void setUser(@Nullable User user) {
		this.user = user;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = StringUtils.abbreviate(content, MAX_CONTENT_LEN);
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public int getReplyCount() {
		return replyCount;
	}

	public void setReplyCount(int replyCount) {
		this.replyCount = replyCount;
	}

	public Mark getMark() {
		return mark;
	}

	public void setMark(Mark mark) {
		this.mark = mark;
	}

	public Collection<CodeCommentReply> getReplies() {
		return replies;
	}

	public void setReplies(Collection<CodeCommentReply> replies) {
		this.replies = replies;
	}

	public Collection<CodeCommentStatusChange> getChanges() {
		return changes;
	}

	public void setChanges(Collection<CodeCommentStatusChange> changes) {
		this.changes = changes;
	}

	public Collection<PendingSuggestionApply> getPendingSuggestionApplies() {
		return pendingSuggestionApplies;
	}

	public void setPendingSuggestionApplies(Collection<PendingSuggestionApply> pendingSuggestionApplies) {
		this.pendingSuggestionApplies = pendingSuggestionApplies;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}
	
	public CompareContext getCompareContext() {
		return compareContext;
	}

	public void setCompareContext(CompareContext compareContext) {
		this.compareContext = compareContext;
	}

	public LastActivity getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(LastActivity lastActivity) {
		this.lastActivity = lastActivity;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}

	public Collection<CodeCommentMention> getMentions() {
		return mentions;
	}

	public void setMentions(Collection<CodeCommentMention> mentions) {
		this.mentions = mentions;
	}

	public boolean isVisitedAfter(Date date) {
		User user = SecurityUtils.getAuthUser();
		if (user != null) {
			Date visitDate = OneDev.getInstance(VisitInfoManager.class).getCodeCommentVisitDate(user, this);
			return visitDate != null && visitDate.getTime()>date.getTime();
		} else {
			return true;
		}
	}
	
	public boolean isValid() {
		return getMissingCommits().isEmpty();
	}

	public Collection<ObjectId> getMissingCommits() {
		GitService gitService = OneDev.getInstance(GitService.class);
		Set<ObjectId> objIds = Sets.newHashSet(ObjectId.fromString(mark.getCommitHash()));

		ObjectId oldCommitId= ObjectId.fromString(compareContext.getOldCommitHash());
		if (!oldCommitId.equals(ObjectId.zeroId()))
			objIds.add(oldCommitId);

		ObjectId newCommitId= ObjectId.fromString(compareContext.getNewCommitHash());
		if (!newCommitId.equals(ObjectId.zeroId()))
			objIds.add(newCommitId);

		return gitService.filterNonExistants(project, objIds);
	}
	
	public static String getChangeObservable(Long commentId) {
		return CodeComment.class.getName() + ":" + commentId;
	}
	
	@Override
	public Project getAttachmentProject() {
		return project;
	}
	
	@Override
	public String getAttachmentGroup() {
		return uuid;
	}

	public List<User> getParticipants() {
		if (participants == null) {
			participants = new LinkedHashSet<>();
			participants.add(getUser());
			for (CodeCommentReply reply: getReplies()) 
				participants.add(reply.getUser());
			var userManager = OneDev.getInstance(UserManager.class);
			participants.remove(userManager.getSystem());
			participants.remove(userManager.getUnknown());
		}
		return new ArrayList<>(participants);
	}
	
}
