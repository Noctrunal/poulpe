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
package org.jtalks.poulpe.web.controller.group;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.jtalks.poulpe.model.entity.Group;
import org.jtalks.poulpe.service.GroupService;
import org.jtalks.poulpe.web.controller.DialogManager;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GroupPresenterTest {
    GroupPresenter presenter;
    @Mock
    DialogManager dialogManager;
    @Mock
    GroupService service;
    @Mock
    GroupViewImpl viewMock;

    @Test(dataProvider = "emptyListProvider")
    public void testInitView(List<Group> returnedGroups) throws Exception {
        when(service.getAllMatchedByName(null)).thenReturn(returnedGroups);
        verify(viewMock).updateView(returnedGroups);
    }

    @DataProvider(name = "emptyListProvider")
    public Object[][] testData() {
        return new Object[][] { { new LinkedList() } };
    }

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        presenter = new GroupPresenter();
        presenter.setDialogManager(dialogManager);
        presenter.setGroupService(service);
        presenter.initView(viewMock);
    }

    @Test
    public void testOnEditGroup() {
        Group group = new Group();
        presenter.onEditGroup(group);
        verify(viewMock).openEditDialog(group);
    }

    @Test
    public void testOnAddGroup() {
        presenter.onAddGroup();
        verify(viewMock).openNewDialog();
    }

}
