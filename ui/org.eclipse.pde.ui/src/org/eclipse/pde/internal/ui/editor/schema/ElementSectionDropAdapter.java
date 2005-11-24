/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

public class ElementSectionDropAdapter extends ViewerDropAdapter {
	private TransferData currentTransfer;
	private ElementSection section;

	public ElementSectionDropAdapter(ElementSection section) {
		super(section.getTreeViewer());
		this.section = section;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	public boolean performDrop(Object data) {
		if (data instanceof Object[]) {
			if (getCurrentOperation() == DND.DROP_LINK) {
				section.doLink(getCurrentTarget(), (Object[])data);
			} else {
				section.doPaste(getCurrentTarget(), (Object[])data);
			}
			return true;
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		currentTransfer = transferType;
		if (!ModelDataTransfer.getInstance().isSupportedType(currentTransfer))
			return false;
		Object cargo = getSelectedObject();
		
		// only way to link is dropping an element onto a compositor or reference
		if (operation == DND.DROP_LINK)
			return (cargo instanceof ISchemaElement
				&& (target instanceof ISchemaCompositor || target instanceof ISchemaObjectReference));
			
		if (cargo instanceof ISchemaObjectReference) {
			// onto a compositor or reference
			if ((target instanceof ISchemaCompositor 
					|| target instanceof ISchemaObjectReference))
				return true;
		} else if (cargo instanceof ISchemaElement) { // droping an element
			// onto a non referenced element
			if (isNonRefElement(target) || target == null)
				return true;
		} else if (cargo instanceof ISchemaCompositor) { // dropping a compositor
			// onto a non referenced element
			if (isNonRefElement(target))
				return true;
		} else if (cargo instanceof ISchemaAttribute) { // dropping an attribute
			// onto a non referenced element or attribute
			if (isNonRefElement(target) || target instanceof ISchemaAttribute)
				return true;
		}
		return false;
	}
	
	private boolean isNonRefElement(Object obj) {
		return (obj instanceof ISchemaElement && !(obj instanceof ISchemaObjectReference));
	}
}
