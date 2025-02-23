package io.onedev.server.web.page.admin.usermanagement.password;

import io.onedev.server.web.component.user.passwordedit.PasswordEditPanel;
import io.onedev.server.web.page.admin.usermanagement.UserPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class UserPasswordPage extends UserPage {
	
	public UserPasswordPage(PageParameters params) {
		super(params);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new WebMarkupContainer("authViaExternalSystemNotice").setVisible(getUser().getPassword() == null));
		add(new PasswordEditPanel("content", userModel));
	}

}
