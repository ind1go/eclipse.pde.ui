/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Page to edit environment and JRE settings for a target definition
 */
public class TargetDefinitionEnvironmentPage extends TargetDefinitionPage {

	// Environment pull-downs
	private Combo fOSCombo;
	private Combo fWSCombo;
	private Combo fArchCombo;
	private Combo fNLCombo;

	// Choices for each pull-down
	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;

	// JRE section
	private Button fDefaultJREButton;
	private Button fNamedJREButton;
	private Button fExecEnvButton;
	private Combo fNamedJREsCombo;
	private Combo fExecEnvsCombo;
	private TreeSet fExecEnvChoices;

	// argument controls
	private Text fProgramArgs;
	private Text fVMArgs;
	private Button fAppendLauncherArgs;

	// implicit dependencies tab
	private TableViewer fElementViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;

	/**
	 * 
	 * @param definition target definition to edit
	 */
	protected TargetDefinitionEnvironmentPage(ITargetDefinition definition) {
		super("targetEnvironment", definition); //$NON-NLS-1$
		setTitle(PDEUIMessages.TargetDefinitionEnvironmentPage_1);
		setDescription(PDEUIMessages.TargetDefinitionEnvironmentPage_2);
		setImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, true));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(comp);
		TabFolder tabs = new TabFolder(comp, SWT.NONE);
		tabs.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem envTab = new TabItem(tabs, SWT.NONE);
		envTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_3);

		Composite container = new Composite(tabs, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 15;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTargetEnvironmentGroup(container);
		createJREGroup(container);
		envTab.setControl(container);

		TabItem argsTab = new TabItem(tabs, SWT.NONE);
		argsTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_4);
		argsTab.setControl(createArgumentsGroup(tabs));

		TabItem depTab = new TabItem(tabs, SWT.NONE);
		depTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_5);
		depTab.setControl(createImplicitTabContents(tabs));

		targetChanged(getTargetDefinition());
	}

	private void createTargetEnvironmentGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_targetEnv);

		initializeChoices();

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_os);

		fOSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fOSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[]) fOSChoices.toArray(new String[fOSChoices.size()]));
		fOSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setOS(getModelValue(fOSCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_ws);

		fWSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fWSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[]) fWSChoices.toArray(new String[fWSChoices.size()]));
		fWSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setWS(getModelValue(fWSCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_arch);

		fArchCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[]) fArchChoices.toArray(new String[fArchChoices.size()]));
		fArchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setArch(getModelValue(fArchCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_nl);

		fNLCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fNLCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));
		fNLCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String value = fNLCombo.getText();
				int index = value.indexOf("-"); //$NON-NLS-1$
				if (index > 0)
					value = value.substring(0, index);
				getTargetDefinition().setNL(getModelValue(value));
			}
		});

	}

	/**
	 * Returns the given string or <code>null</code> if empty to set a value in the
	 * target definition.
	 * 
	 * @param value
	 * @return
	 */
	private String getModelValue(String value) {
		if (value != null) {
			value = value.trim();
			if (value.length() == 0) {
				return null;
			}
		}
		return value;
	}

	private void addExtraChoices(Set set, String preference) {
		StringTokenizer tokenizer = new StringTokenizer(preference, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			set.add(tokenizer.nextToken().trim());
		}
	}

	private void initializeChoices() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();

		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		addExtraChoices(fOSChoices, preferences.getString(ICoreConstants.OS_EXTRA));

		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		addExtraChoices(fWSChoices, preferences.getString(ICoreConstants.WS_EXTRA));

		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		addExtraChoices(fArchChoices, preferences.getString(ICoreConstants.ARCH_EXTRA));

		fNLChoices = new TreeSet();
		initializeAllLocales();
	}

	private void initializeAllLocales() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String[] nl = LocaleUtil.getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		addExtraChoices(fNLChoices, preferences.getString(ICoreConstants.NL_EXTRA));
	}

	private void createJREGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_jreTitle);

		initializeJREValues();

		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.JRESection_description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fDefaultJREButton = new Button(group, SWT.RADIO);
		fDefaultJREButton.setText(PDEUIMessages.JRESection_defaultJRE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fDefaultJREButton.setLayoutData(gd);
		fDefaultJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newDefaultJREContainerPath());
			}
		});

		fNamedJREButton = new Button(group, SWT.RADIO);
		fNamedJREButton.setText(PDEUIMessages.JRESection_JREName);
		fNamedJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fNamedJREsCombo = new Combo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fNamedJREsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] installs = VMUtil.getVMInstallNames();
		fNamedJREsCombo.setItems(installs);
		fNamedJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fExecEnvButton = new Button(group, SWT.RADIO);
		fExecEnvButton.setText(PDEUIMessages.JRESection_ExecutionEnv);
		fExecEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

		fExecEnvsCombo = new Combo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fExecEnvsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fExecEnvsCombo.setItems((String[]) fExecEnvChoices.toArray(new String[fExecEnvChoices.size()]));
		fExecEnvsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

	}

	/**
	 * Initializes the combo with possible execution enviroments
	 */
	protected void initializeJREValues() {
		fExecEnvChoices = new TreeSet();
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		for (int i = 0; i < envs.length; i++)
			fExecEnvChoices.add(envs[i].getId());
	}

	protected void updateJREWidgets() {
		fNamedJREsCombo.setEnabled(fNamedJREButton.getSelection());
		fExecEnvsCombo.setEnabled(fExecEnvButton.getSelection());
	}

	private Control createArgumentsGroup(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		Label description = new Label(container, SWT.WRAP);
		description.setText(PDEUIMessages.JavaArgumentsTab_description);
		GridData gd = new GridData();
		gd.widthHint = 450;
		description.setLayoutData(gd);

		Group programGroup = new Group(container, SWT.NONE);
		programGroup.setLayout(new GridLayout());
		programGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		programGroup.setText(PDEUIMessages.JavaArgumentsTab_progamArgsGroup);

		fProgramArgs = new Text(programGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		gd.heightHint = 60;
		fProgramArgs.setLayoutData(gd);
		fProgramArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setProgramArguments(fProgramArgs.getText().trim());
			}
		});

		Button programVars = new Button(programGroup, SWT.NONE);
		programVars.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		programVars.setText(PDEUIMessages.JavaArgumentsTab_programVariables);
		programVars.addSelectionListener(getListener(fProgramArgs));

		Group vmGroup = new Group(container, SWT.NONE);
		vmGroup.setLayout(new GridLayout(2, false));
		vmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		vmGroup.setText(PDEUIMessages.JavaArgumentsTab_vmArgsGroup);

		fVMArgs = new Text(vmGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		gd.heightHint = 60;
		gd.horizontalSpan = 2;
		fVMArgs.setLayoutData(gd);
		fVMArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setVMArguments(fVMArgs.getText().trim());
			}
		});

		fAppendLauncherArgs = new Button(vmGroup, SWT.CHECK);
		fAppendLauncherArgs.setText(PDEUIMessages.JavaArgumentsTab_appendLauncherIni);

		Button vmVars = new Button(vmGroup, SWT.NONE);
		vmVars.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		vmVars.setText(PDEUIMessages.JavaArgumentsTab_vmVariables);
		vmVars.addSelectionListener(getListener(fVMArgs));
		return container;
	}

	protected SelectionListener getListener(final Text textControl) {
		return new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					textControl.insert(variable);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
	}

	private Control createImplicitTabContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createImpLabel(container);
		createImpTable(container);
		createImpButtons(container);
		// TODO: PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.IMPLICIT_PLUGINS_PREFERENCE_PAGE);
		return container;
	}

	private void createImpLabel(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.TargetImplicitPluginsTab_desc);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}

	private void createImpTable(Composite container) {
		fElementViewer = new TableViewer(container, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fElementViewer.getControl().setLayoutData(gd);
		fElementViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				BundleInfo[] bundles = getTargetDefinition().getImplicitDependencies();
				if (bundles == null) {
					return new BundleInfo[0];
				}
				return bundles;
			}
		});
		fElementViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof BundleInfo) {
					return ((BundleInfo) element).getSymbolicName();
				}
				return super.getText(element);
			}
			// TODO: labels
		});
		fElementViewer.setInput(PDEPlugin.getDefault());
		fElementViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				BundleInfo bundle1 = (BundleInfo) e1;
				BundleInfo bundle2 = (BundleInfo) e2;
				return super.compare(viewer, bundle1.getSymbolicName(), bundle2.getSymbolicName());
			}
		});
		fElementViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateImpButtons();
			}
		});
		fElementViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL && e.stateMask == 0) {
					handleRemove();
				}
			}
		});

	}

	private void createImpButtons(Composite container) {
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.SourceBlock_add);
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		fRemoveAllButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveAllButton.setText(PDEUIMessages.TargetImplicitPluginsTab_removeAll3);
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveAllButton);
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		updateImpButtons();
	}

	protected void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());

		dialog.setElements(getValidBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title);
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			ArrayList pluginsToAdd = new ArrayList();
			for (int i = 0; i < models.length; i++) {
				BundleDescription desc = ((BundleDescription) models[i]);
				pluginsToAdd.add(new BundleInfo(desc.getSymbolicName(), null, null, BundleInfo.NO_LEVEL, false));
			}
			Set allDependencies = new HashSet();
			allDependencies.addAll(pluginsToAdd);
			BundleInfo[] currentBundles = getTargetDefinition().getImplicitDependencies();
			if (currentBundles != null) {
				allDependencies.addAll(Arrays.asList(currentBundles));
			}
			getTargetDefinition().setImplicitDependencies((BundleInfo[]) allDependencies.toArray(new BundleInfo[allDependencies.size()]));
			fElementViewer.refresh();
			// update target
		}
	}

	/**
	 * Gets a list of all the bundles that can be added as implicit dependencies
	 * @return list of possible dependencies
	 */
	protected BundleDescription[] getValidBundles() {
		BundleInfo[] current = getTargetDefinition().getImplicitDependencies();
		Set currentBundles = new HashSet();
		if (current != null) {
			for (int i = 0; i < current.length; i++) {
				currentBundles.add(current[i].getSymbolicName());
			}
		}

		// TODO Do we want to get the possible models from the plugin registry?  Would be better to get the bundles from the editor's target definition?
		IPluginModelBase[] models = PluginRegistry.getActiveModels(false);
		Set result = new HashSet();
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = models[i].getBundleDescription();
			if (desc != null) {
				if (!currentBundles.contains(desc.getSymbolicName()))
					result.add(desc);
			}
		}

		return (BundleDescription[]) result.toArray((new BundleDescription[result.size()]));
	}

	private void handleRemove() {
		LinkedList bundles = new LinkedList();
		bundles.addAll(Arrays.asList(getTargetDefinition().getImplicitDependencies()));
		Object[] removeBundles = ((IStructuredSelection) fElementViewer.getSelection()).toArray();
		if (removeBundles.length > 0) {
			for (int i = 0; i < removeBundles.length; i++) {
				if (removeBundles[i] instanceof BundleInfo) {
					bundles.remove(removeBundles[i]);
				}
			}
			getTargetDefinition().setImplicitDependencies((BundleInfo[]) bundles.toArray((new BundleInfo[bundles.size()])));
			fElementViewer.refresh();
		}
	}

	private void handleRemoveAll() {
		getTargetDefinition().setImplicitDependencies(null);
		fElementViewer.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionPage#targetChanged()
	 */
	protected void targetChanged(ITargetDefinition definition) {
		super.targetChanged(definition);
		if (definition != null) {
			String presetValue = (definition.getOS() == null) ? "" : definition.getOS(); //$NON-NLS-1$
			fOSCombo.setText(presetValue);
			presetValue = (definition.getWS() == null) ? "" : definition.getWS(); //$NON-NLS-1$
			fWSCombo.setText(presetValue);
			presetValue = (definition.getArch() == null) ? "" : definition.getArch(); //$NON-NLS-1$
			fArchCombo.setText(presetValue);
			presetValue = (definition.getNL() == null) ? "" : LocaleUtil.expandLocaleName(definition.getNL()); //$NON-NLS-1$
			fNLCombo.setText(presetValue);

			IPath jrePath = definition.getJREContainer();
			if (jrePath == null || jrePath.equals(JavaRuntime.newDefaultJREContainerPath())) {
				fDefaultJREButton.setSelection(true);
			} else {
				String ee = JavaRuntime.getExecutionEnvironmentId(jrePath);
				if (ee != null) {
					fExecEnvButton.setSelection(true);
					fExecEnvsCombo.select(fExecEnvsCombo.indexOf(ee));
				} else {
					String vm = JavaRuntime.getVMInstallName(jrePath);
					if (vm != null) {
						fNamedJREButton.setSelection(true);
						fNamedJREsCombo.select(fNamedJREsCombo.indexOf(vm));
					}
				}
			}

			if (fExecEnvsCombo.getSelectionIndex() == -1)
				fExecEnvsCombo.setText(fExecEnvChoices.first().toString());

			if (fNamedJREsCombo.getSelectionIndex() == -1)
				fNamedJREsCombo.setText(VMUtil.getDefaultVMInstallName());

			updateJREWidgets();

			presetValue = (definition.getProgramArguments() == null) ? "" : definition.getProgramArguments(); //$NON-NLS-1$
			fProgramArgs.setText(presetValue);
			presetValue = (definition.getVMArguments() == null) ? "" : definition.getVMArguments(); //$NON-NLS-1$
			fVMArgs.setText(presetValue);

		}
	}

	private void updateImpButtons() {
		boolean empty = fElementViewer.getSelection().isEmpty();
		fRemoveButton.setEnabled(!empty);
		boolean hasElements = fElementViewer.getTable().getItemCount() > 0;
		fRemoveAllButton.setEnabled(hasElements);
	}

}
