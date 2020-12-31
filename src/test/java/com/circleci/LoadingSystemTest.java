package com.circleci;

import com.circleci.api.model.Build;
import com.circleci.api.model.Project;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.ui.CollectionListModel;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LoadingSystemTest {

    private IdeaProjectTestFixture projectFixture;

    private TestAPIServer testApiServer;

    @Before
    public void setUp() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR);

        projectFixture = fixtureBuilder.getFixture();
        projectFixture.setUp();

        testApiServer = new TestAPIServer();

        CircleCISettings settings = CircleCISettings.getInstance();
        settings.serverUrl = testApiServer.setup();
    }

    @Test
    public void reload() throws InterruptedException {
        SingleEventWaiter eventWaiter = new SingleEventWaiter(projectFixture.getProject());

        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, projectFixture.getProject());
        Project project = new Project("circleci", "daproject", "github");

        testApiServer.setResponse(buildSequence(3, 2, 1));

        // action
        listLoader.init();
        sendProjectChangedEvent(null, project);

        eventWaiter.waitForEvent(CircleCIEvents.LIST_UPDATED_TOPIC, () -> eventWaiter.eventSeen.set(true));

        assertEquals(3, listModel.getSize());
    }

    @Test
    public void loadMore() throws InterruptedException {
        SingleEventWaiter eventWaiter = new SingleEventWaiter(projectFixture.getProject());
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectFixture.getProject());
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, projectFixture.getProject());

        listModel.add(buildSequence(5, 4));

        testApiServer.setResponse(buildSequence(3, 2, 1));

        projectSettings.activeProject = new Project("circleci", "daproject", "github");

        // action
        listLoader.loadMore();

        eventWaiter.waitForEvent(CircleCIEvents.LIST_UPDATED_TOPIC, () -> eventWaiter.eventSeen.set(true));

        assertEquals(5, listModel.getSize());
    }

    @Test
    public void loadNewAndUpdated() throws InterruptedException {
        SingleEventWaiter eventWaiter = new SingleEventWaiter(projectFixture.getProject());
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectFixture.getProject());
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, projectFixture.getProject());

        listModel.add(buildSequence(3, 2, 1));

        List<Build> response = buildSequence(4, 3, 2);
        response.get(1).setStatus("success");
        testApiServer.setResponse(response);

        projectSettings.activeProject = new Project("circleci", "daproject", "github");

        // action
        listLoader.loadNewAndUpdated();

        eventWaiter.waitForEvent(CircleCIEvents.NEW_ITEMS_STORED_TOPIC, () -> eventWaiter.eventSeen.set(true));

        listLoader.merge();

        assertEquals(4, listModel.getSize());
        assertEquals(listModel.getElementAt(1).getStatus(), "success");
    }


    public List<Build> buildSequence(int... buildNumbers) {
        List<Build> builds = new ArrayList<>();
        for (int buildNumber : buildNumbers) {
            builds.add(build(buildNumber));
        }
        return builds;
    }

    private Build build(int buildNum) {
        Build build = new Build();
        build.setBuildNumber(buildNum);
        build.setStatus("running");
        build.setOrganization("circleci");
        build.setProject("daproject");
        build.setUrl("https//:" + buildNum);
        return build;
    }

    private void sendProjectChangedEvent(Project previous, Project current) {
        projectFixture.getProject()
                .getMessageBus()
                .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC).projectChanged(
                new ActiveProjectChangeEvent(null, current)
        );
    }

}