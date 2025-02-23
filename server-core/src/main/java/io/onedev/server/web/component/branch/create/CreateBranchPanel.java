package io.onedev.server.web.component.branch.create;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.FencedFeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import io.onedev.server.OneDev;
import io.onedev.server.git.GitUtils;
import io.onedev.server.git.service.GitService;
import io.onedev.server.model.Project;
import io.onedev.server.model.User;
import io.onedev.server.security.SecurityUtils;
import io.onedev.server.util.Path;
import io.onedev.server.util.PathNode;
import io.onedev.server.web.editable.BeanContext;
import io.onedev.server.web.editable.BeanEditor;

public abstract class CreateBranchPanel extends Panel {

	private final IModel<Project> projectModel;
	
	private final String revision;
	
	private CreateBranchBean helperBean = new CreateBranchBean();
	
	public CreateBranchPanel(String id, IModel<Project> projectModel, String revision) {
		super(id);
		this.projectModel = projectModel;
		this.revision = revision;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		Form<?> form = new Form<Void>("form");
		form.setOutputMarkupId(true);
		form.add(new FencedFeedbackPanel("feedback", form));
		
		BeanEditor editor;
		form.add(editor = BeanContext.edit("editor", helperBean));
		
		form.add(new AjaxButton("create") {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				super.onSubmit(target, form);
				
				Project project = projectModel.getObject();
				User user = SecurityUtils.getAuthUser();
				String branchName = helperBean.getName();
				if (project.getObjectId(GitUtils.branch2ref(branchName), false) != null) {
					editor.error(new Path(new PathNode.Named("name")), 
							"Branch '" + branchName + "' already exists, please choose a different name");
					target.add(form);
				} else if (project.getBranchProtection(branchName, user).isPreventCreation()) {
					editor.error(new Path(new PathNode.Named("name")), 
							"Creation of this branch is prohibited per branch protection rule");
					target.add(form);
				} else {
					if (!project.isCommitSignatureRequirementSatisfied(user, 
							branchName, project.getRevCommit(revision, true))) {
						editor.error(new Path(new PathNode.Named("name")), 
								"Valid signature required for head commit of this branch per branch protection rule");
						target.add(form);
					} else {
						OneDev.getInstance(GitService.class).createBranch(project, branchName, revision);
						onCreate(target, branchName);
					}
				}
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				super.onError(target, form);
				target.add(form);
			}

		});
		form.add(new AjaxLink<Void>("cancel") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancel(target);
			}
			
		});
		
		form.add(new AjaxLink<Void>("close") {

			@Override
			public void onClick(AjaxRequestTarget target) {
				onCancel(target);
			}
			
		});
		
		add(form);
	}
	
	protected abstract void onCreate(AjaxRequestTarget target, String branch);
	
	protected abstract void onCancel(AjaxRequestTarget target);

	@Override
	protected void onDetach() {
		projectModel.detach();
		
		super.onDetach();
	}

}
