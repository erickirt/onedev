package io.onedev.server.rest.resource;

import static io.onedev.server.security.SecurityUtils.canAccessIssue;
import static io.onedev.server.security.SecurityUtils.canModifyOrDelete;
import static io.onedev.server.security.SecurityUtils.getUser;
import static io.onedev.server.security.SecurityUtils.isAdministrator;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.UnauthorizedException;

import io.onedev.server.entitymanager.IssueCommentManager;
import io.onedev.server.model.IssueComment;
import io.onedev.server.rest.annotation.Api;
import io.onedev.server.security.SecurityUtils;

@Api(order=2300)
@Path("/issue-comments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class IssueCommentResource {

	private final IssueCommentManager commentManager;

	@Inject
	public IssueCommentResource(IssueCommentManager commentManager) {
		this.commentManager = commentManager;
	}

	@Api(order=100)
	@Path("/{commentId}")
	@GET
	public IssueComment get(@PathParam("commentId") Long commentId) {
		IssueComment comment = commentManager.load(commentId);
    	if (!SecurityUtils.canAccessProject(comment.getIssue().getProject()))  
			throw new UnauthorizedException();
    	return comment;
	}
	
	@Api(order=200, description="Create new issue comment")
	@POST
	public Long create(@NotNull IssueComment comment) {
		if (!canAccessIssue(comment.getIssue()) 
				|| !isAdministrator() && !comment.getUser().equals(getUser())) {
			throw new UnauthorizedException();
		}
		commentManager.create(comment);
		return comment.getId();
	}

	@Api(order=250, description="Update issue comment of specified id")
	@Path("/{commentId}")
	@POST
	public Response update(@PathParam("commentId") Long commentId, @NotNull IssueComment comment) {
		if (!canModifyOrDelete(comment)) 
			throw new UnauthorizedException();
		commentManager.update(comment);
		return Response.ok().build();
	}
	
	@Api(order=300)
	@Path("/{commentId}")
	@DELETE
	public Response delete(@PathParam("commentId") Long commentId) {
		IssueComment comment = commentManager.load(commentId);
    	if (!canModifyOrDelete(comment)) 
			throw new UnauthorizedException();
		commentManager.delete(comment);
		return Response.ok().build();
	}
	
}
