package io.onedev.server.web.editable.jobprivilege;

import com.google.common.base.Joiner;
import io.onedev.server.model.support.role.JobPrivilege;
import io.onedev.server.util.CollectionUtils;
import io.onedev.server.web.ajaxlistener.ConfirmClickListener;
import io.onedev.server.web.behavior.NoRecordsBehavior;
import io.onedev.server.web.behavior.sortable.SortBehavior;
import io.onedev.server.web.behavior.sortable.SortPosition;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.svg.SpriteImage;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyUpdating;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class JobPrivilegeListEditPanel extends PropertyEditor<List<Serializable>> {

	private final List<JobPrivilege> privileges;
	
	public JobPrivilegeListEditPanel(String id, PropertyDescriptor propertyDescriptor, IModel<List<Serializable>> model) {
		super(id, propertyDescriptor, model);
		
		privileges = new ArrayList<>();
		for (Serializable each: model.getObject()) {
			privileges.add((JobPrivilege) each);
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new ModalLink("addNew") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new JobPrivilegeEditPanel(id, privileges, -1) {

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}

					@Override
					protected void onSave(AjaxRequestTarget target) {
						markFormDirty(target);
						modal.close();
						onPropertyUpdating(target);
						target.add(JobPrivilegeListEditPanel.this);
					}

				};
			}
			
		});
		
		List<IColumn<JobPrivilege, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<JobPrivilege>> cellItem, String componentId, IModel<JobPrivilege> rowModel) {
				cellItem.add(new SpriteImage(componentId, "grip") {

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						tag.setName("svg");
						tag.put("class", "icon drag-indicator");
					}

				});
			}

			@Override
			public String getCssClass() {
				return "minimum actions";
			}

		});		
		
		columns.add(new AbstractColumn<>(Model.of("Job Names")) {

			@Override
			public void populateItem(Item<ICellPopulator<JobPrivilege>> cellItem, String componentId, IModel<JobPrivilege> rowModel) {
				cellItem.add(new Label(componentId, rowModel.getObject().getJobNames()));
			}
		});		
		
		columns.add(new AbstractColumn<>(Model.of("Privilege")) {

			@Override
			public void populateItem(Item<ICellPopulator<JobPrivilege>> cellItem, String componentId, IModel<JobPrivilege> rowModel) {
				JobPrivilege privilege = rowModel.getObject();
				if (privilege.isManageJob()) {
					cellItem.add(new Label(componentId, "manage job"));
				} else if (privilege.isRunJob()) {
					cellItem.add(new Label(componentId, "run job"));
				} else {
					var accessibles = new ArrayList<>();
					if (privilege.isAccessLog())
						accessibles.add("log");
					if (privilege.isAccessPipeline())
						accessibles.add("pipeline");
					accessibles.add("artifacts");
					if (privilege.getAccessibleReports() != null)
						accessibles.add("reports:" + privilege.getAccessibleReports());
					cellItem.add(new Label(componentId, "access [" + Joiner.on(", ").join(accessibles) + "]"));
				}
			}
		});		
		
		columns.add(new AbstractColumn<>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<JobPrivilege>> cellItem, String componentId, IModel<JobPrivilege> rowModel) {
				Fragment fragment = new Fragment(componentId, "actionColumnFrag", JobPrivilegeListEditPanel.this);
				fragment.add(new ModalLink("edit") {

					@Override
					protected Component newContent(String id, ModalPanel modal) {
						return new JobPrivilegeEditPanel(id, privileges, cellItem.findParent(Item.class).getIndex()) {

							@Override
							protected void onCancel(AjaxRequestTarget target) {
								modal.close();
							}

							@Override
							protected void onSave(AjaxRequestTarget target) {
								markFormDirty(target);
								modal.close();
								onPropertyUpdating(target);
								target.add(JobPrivilegeListEditPanel.this);
							}

						};
					}

				});
				fragment.add(new AjaxLink<Void>("delete") {

					@Override
					protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
						super.updateAjaxAttributes(attributes);
						attributes.getAjaxCallListeners().add(new ConfirmClickListener("Do you really want to delete this privilege?"));
					}

					@Override
					public void onClick(AjaxRequestTarget target) {
						markFormDirty(target);
						privileges.remove(rowModel.getObject());
						onPropertyUpdating(target);
						target.add(JobPrivilegeListEditPanel.this);
					}

				});
				cellItem.add(fragment);
			}

			@Override
			public String getCssClass() {
				return "actions";
			}

		});		
		
		IDataProvider<JobPrivilege> dataProvider = new ListDataProvider<>() {

			@Override
			protected List<JobPrivilege> getData() {
				return privileges;
			}

		};
		
		DataTable<JobPrivilege, Void> dataTable;
		add(dataTable = new DataTable<>("privileges", columns, dataProvider, Integer.MAX_VALUE));
		dataTable.addTopToolbar(new HeadersToolbar<>(dataTable, null));
		dataTable.addBottomToolbar(new NoRecordsToolbar(dataTable, Model.of("Unspecified")));
		dataTable.add(new NoRecordsBehavior());
		
		dataTable.add(new SortBehavior() {

			@Override
			protected void onSort(AjaxRequestTarget target, SortPosition from, SortPosition to) {
				CollectionUtils.move(privileges, from.getItemIndex(), to.getItemIndex());
				onPropertyUpdating(target);
				target.add(JobPrivilegeListEditPanel.this);
			}
			
		}.sortable("tbody"));
	}

	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);
		
		if (event.getPayload() instanceof PropertyUpdating) {
			event.stop();
			onPropertyUpdating(((PropertyUpdating)event.getPayload()).getHandler());
		}		
	}

	@Override
	protected List<Serializable> convertInputToValue() throws ConversionException {
		List<Serializable> value = new ArrayList<>();
		for (JobPrivilege each: privileges)
			value.add(each);
		return value;
	}

	@Override
	public boolean needExplicitSubmit() {
		return true;
	}
	
}
