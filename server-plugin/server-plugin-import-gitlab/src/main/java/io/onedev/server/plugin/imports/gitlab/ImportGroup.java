package io.onedev.server.plugin.imports.gitlab;

import java.io.Serializable;
import java.util.Map;

import io.onedev.server.annotation.ChoiceProvider;
import io.onedev.server.annotation.Editable;
import io.onedev.server.util.ComponentContext;
import io.onedev.server.web.editable.BeanEditor;

@Editable
public class ImportGroup implements Serializable {

	private static final long serialVersionUID = 1L;
	
	ImportServer server;
	
	private String groupId;
		
	@Editable(order=100, name="GitLab Group", description="Specify group to import from. "
			+ "Leave empty to import from projects under current account")
	@ChoiceProvider("getGroupChoices")
	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	@SuppressWarnings("unused")
	private static Map<String, String> getGroupChoices() {
		BeanEditor editor = ComponentContext.get().getComponent().findParent(BeanEditor.class);
		ImportGroup setting = (ImportGroup) editor.getModelObject();
		return setting.server.listGroups();
	}
	
}
