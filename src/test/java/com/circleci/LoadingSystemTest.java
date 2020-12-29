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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    public void initialLoad() throws InterruptedException {
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectFixture.getProject());
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, projectFixture.getProject());

        testApiServer.setResponse(buildSequence(3, 2, 1));

        projectSettings.activeProject = new Project("circleci", "daproject", "github");

        // action
        listLoader.init(projectFixture.getProject());

        waitAndCheck(() -> {
            return false;
        }, 2, ChronoUnit.SECONDS);

        assertEquals(3, listModel.getSize());
    }

    @Test
    public void loadMore() throws InterruptedException {
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectFixture.getProject());
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, projectFixture.getProject());

        listModel.add(buildSequence(5, 4));

        testApiServer.setResponse(buildSequence(3, 2, 1));

        projectSettings.activeProject = new Project("circleci", "daproject", "github");

        // action
        listLoader.loadMore();

        waitAndCheck(() -> {
            return false;
        }, 2, ChronoUnit.SECONDS);

        assertEquals(5, listModel.getSize());
    }

    @Test
    public void loadNew() throws InterruptedException {
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

        waitAndCheck(() -> {
            return false;
        }, 2, ChronoUnit.SECONDS);

        listLoader.merge();

        waitAndCheck(() -> {
            return false;
        }, 2, ChronoUnit.SECONDS);

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

    public void waitAndCheck(Supplier<Boolean> operation, int timeout, TemporalUnit unit) throws InterruptedException {
        Instant start = Instant.now();
        Instant deadline = start.plus(timeout, unit);
        while (true) {
            boolean result = operation.get();
            if (result || Instant.now().isAfter(deadline)) {
                return;
            }
            Thread.sleep(500L);
        }
    }
}