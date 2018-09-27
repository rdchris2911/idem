package net.mguenther.idem.flake;

import net.mguenther.idem.Wait;
import net.mguenther.idem.encoder.Base62Encoder;
import net.mguenther.idem.provider.LinearTimeProvider;
import net.mguenther.idem.provider.StaticWorkerIdProvider;
import net.mguenther.idem.provider.TimeProvider;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class Flake64STest {

    private final TimeProvider timeProvider = new LinearTimeProvider();

    @Test
    public void shouldIsolateProcessesProperlyByWorkerIds() throws InterruptedException {

        IdGeneratorWorker<String> worker1 = new IdGeneratorWorker<>(new Flake64S(timeProvider, new StaticWorkerIdProvider("A"), new Base62Encoder()));
        IdGeneratorWorker<String> worker2 = new IdGeneratorWorker<>(new Flake64S(timeProvider, new StaticWorkerIdProvider("B"), new Base62Encoder()));

        Thread workerThread1 = new Thread(worker1);
        Thread workerThread2 = new Thread(worker2);

        workerThread1.start();
        workerThread2.start();

        Wait.delay(1, TimeUnit.SECONDS);

        worker1.stop();
        worker2.stop();

        workerThread1.join(1_000);
        workerThread2.join(1_000);

        List<String> generatedIdsByWorker1 = worker1.getGeneratedIds();
        List<String> generatedIdsByWorker2 = worker2.getGeneratedIds();

        assertFalse(generatedIdsByWorker1.isEmpty());
        assertFalse(generatedIdsByWorker2.isEmpty());

        Set<String> combinedGeneratedIds = new HashSet<>();
        combinedGeneratedIds.addAll(generatedIdsByWorker1);
        combinedGeneratedIds.addAll(generatedIdsByWorker2);

        // the combined set of IDs would be smaller in size than the sum of the individual lists of
        // generated IDs, if we'd experience any duplicates between the IDs generated by worker 1
        // and those generated by worker 2
        assertThat(combinedGeneratedIds.size(), is(generatedIdsByWorker1.size() + generatedIdsByWorker2.size()));
    }
}
