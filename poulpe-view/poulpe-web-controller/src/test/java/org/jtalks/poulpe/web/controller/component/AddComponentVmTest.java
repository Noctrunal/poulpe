/**
 * Copyright (C) 2011  JTalks.org Team
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jtalks.poulpe.web.controller.component;

import org.jtalks.poulpe.service.ComponentService;
import org.jtalks.poulpe.web.controller.SelectedEntity;
import org.jtalks.poulpe.web.controller.WindowManager;
import org.jtalks.poulpe.web.controller.component.dialogs.AddComponentVm;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;

/**
 * @author Alexey Grigorev
 * @author Kazantcev Leonid
 */
public class AddComponentVmTest {

    AddComponentVm addComponentVm;

    @Mock
    ComponentService componentService;
    @Mock
    WindowManager windowManager;
    @Mock
    SelectedEntity component;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        addComponentVm = new AddComponentVm(componentService, windowManager, component, new ComponentList());
    }

    @Test
    public void cancelEdit() {
        addComponentVm.cancelEdit();
        verify(windowManager).open(ComponentsVm.COMPONENTS_PAGE_LOCATION);
    }

    @Test
    public void createComponent() throws Exception {
        addComponentVm.createComponent();
        verify(componentService).addComponent(addComponentVm.getComponent());
        verify(windowManager).open(ComponentsVm.COMPONENTS_PAGE_LOCATION);
    }

    @Test
    public void getAvailableComponentTypes() {
        addComponentVm.getAvailableComponentTypes();
        verify(componentService).getAvailableTypes();
    }

    @Test
    public void openWindowForAdding() {
        AddComponentVm.openWindowForAdding(windowManager);
        verify(windowManager).open(AddComponentVm.ADD_COMPONENT_LOCATION);
    }
}
