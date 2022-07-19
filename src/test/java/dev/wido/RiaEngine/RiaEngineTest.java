package dev.wido.RiaEngine;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class RiaEngineTest {
    @BeforeEach
    void setUp() throws Exception {
        var instance = RiaEngine.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        var systemsClass = RiaEngine.class.getDeclaredClasses()[0];
        var systemsField = systemsClass.getDeclaredField("systems");
        systemsField.setAccessible(true);
        systemsField.set(null, null);
    }

    @Test
    void riaEngineSingletonInstancesAreTheSame() {
        var ria1 = RiaEngine.get();
        var ria2 = RiaEngine.get();
        assertThat(ria1).isSameAs(ria2);
    }

    @Test
    void riaEngineDetectsAndRegistersBothExecOnceSystems() throws Exception {
        var systemsClass = RiaEngine.class.getDeclaredClasses()[0];
        var systemsField = systemsClass.getDeclaredField("systems");
        systemsField.setAccessible(true);

        RiaEngine.get();
        var systemsList = (List<Runnable>)systemsField.get(null);

        var execOnceSystem = (Runnable)systemsClass
            .getDeclaredField("ExecOnceSystem").get(null);
        var parallelExecOnceSystem = (Runnable)systemsClass
            .getDeclaredField("ParallelExecOnceSystem").get(null);

        assertThat(systemsList).contains(execOnceSystem, parallelExecOnceSystem);
    }

    @Test
    void riaEngineSetupWindowEmptyCreatesConfigTest() {
        var riaSpy = spy(RiaEngine.get());
        doNothing().when(riaSpy).setupWindow(any(Lwjgl3ApplicationConfiguration.class));

        riaSpy.setupWindow();

        verify(riaSpy).setupWindow(any(Lwjgl3ApplicationConfiguration.class));
    }

    @Test
    void riaEngineSetupWindowWithConfigActuallyCreatesApplicationTest() {
        var riaSpy = spy(RiaEngine.get());

        List<?> constructed1;
        try (var appMock = mockConstruction(Lwjgl3Application.class)) {
            riaSpy.setupWindow();
            constructed1 = List.copyOf(appMock.constructed());
        }

        Exception e;
        List<?> constructed2;
        try (var appMock = mockConstruction(Lwjgl3Application.class)) {
            e = catchException(riaSpy::setupWindow);
            constructed2 = appMock.constructed();
        }

        assertThat(constructed1).hasSize(1);
        assertThat(e).isInstanceOf(IllegalStateException.class);
        assertThat(constructed2).isEmpty();
    }

    @Test
    void riaEngineBasicScheduleOnceActuallyWorks() {
        var riaSpy = spy(RiaEngine.get());
        var systemMock = mock(Runnable.class);

        riaSpy.scheduleOnce(systemMock);
        riaSpy.scheduleOnce(systemMock);
        riaSpy.schedulerTick();
        riaSpy.schedulerTick();
        riaSpy.schedulerTick();

        verify(systemMock, times(2)).run();
    }

    @Test
    void riaEngineParallelScheduleOnceActuallyWorks() {
        var riaSpy = spy(RiaEngine.get());
        var systemMock = mock(Runnable.class);

        riaSpy.parallelScheduleOnce(systemMock, systemMock, systemMock);
        riaSpy.parallelScheduleOnce(systemMock);
        riaSpy.schedulerTick();
        riaSpy.schedulerTick();

        verify(systemMock, times(4)).run();
    }
}
