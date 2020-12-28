package com.circleci;

import com.circleci.api.model.Build;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.CollectionListModel;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;

import java.util.LinkedList;


@Ignore // TODO Undo after refactor
public class ListLoaderTest extends BasePlatformTestCase {

    public void testAppendingOnMoreLocalAndServerStateConsistant() {
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = listLoader(listModel);

        listModel.add(build(3));
        listModel.add(build(2));

        LinkedList<Build> fetchedBuilds = new LinkedList<>();
        fetchedBuilds.add(build(1));

        listLoader.loadRequestActionAfterLoad(LoadRequests.more(), fetchedBuilds);

        assertEquals(3, listModel.getSize());
        assertEquals(listModel.getElementAt(listModel.getSize() - 1), fetchedBuilds.getLast());
    }

    public void testAppendingOnMoreServerWithNewBuilds() {
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = listLoader(listModel);

        listModel.add(build(3));
        listModel.add(build(2));

        LinkedList<Build> fetchedBuilds = new LinkedList<>();
        fetchedBuilds.add(build(2));
        fetchedBuilds.add(build(1));

        listLoader.loadRequestActionAfterLoad(LoadRequests.more(), fetchedBuilds);

        assertEquals(3, listModel.getSize());
        assertEquals(listModel.getElementAt(listModel.getSize() - 1), fetchedBuilds.getLast());
    }

    public void testAppendingOnMoreServerWithNewBuildsShiftedSoNothingToAppend() {
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = listLoader(listModel);

        listModel.add(build(3));
        listModel.add(build(2));

        LinkedList<Build> fetchedBuilds = new LinkedList<>();
        fetchedBuilds.add(build(3));
        fetchedBuilds.add(build(2));

        listLoader.loadRequestActionAfterLoad(LoadRequests.more(), fetchedBuilds);

        assertEquals(2, listModel.getSize());
        assertEquals(listModel.getElementAt(listModel.getSize() - 1), fetchedBuilds.getLast());
    }

    private Build build(int buildNum) {
        Build build = new Build();
        build.setBuildNumber(buildNum);
        build.setOrganization("circleci");
        build.setProject("circle");
        build.setUrl("https:" + buildNum);
        return build;
    }


    @NotNull
    private ListLoader listLoader(CollectionListModel<Build> listModel) {
        return new ListLoader(listModel, null);
    }

}