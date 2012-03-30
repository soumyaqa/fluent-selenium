package org.seleniumhq.selenium.fluent;

import org.seleniumhq.selenium.fluent.recording.OnFluentSomething;
import org.seleniumhq.selenium.fluent.recording.RetryStrategy;

import java.util.List;

public class FluentRecording {

    private final List<OnFluentSomething> onFluentSomethings;
    private final RetryStrategy[] retryStrategies;
    private final int millisToSleep;

    public FluentRecording(List<OnFluentSomething> onFluentSomethings, int millisToSleep, RetryStrategy... retryStrategies) {
        this.onFluentSomethings = onFluentSomethings;
        this.retryStrategies = retryStrategies;
        this.millisToSleep = millisToSleep;
    }

    public Object playback(FluentWebDriver driver) {
        long start = System.currentTimeMillis();
        int retries = 0;
        RuntimeException lastRE = null;
        AssertionError lastAE = null;
        boolean goAgain = true;
        while (goAgain) {
            lastAE = null;
            lastRE = null;
            goAgain = false;
            try {
                Object last = driver;
                for (OnFluentSomething o : onFluentSomethings) {
                    last = o.dispatch(last);
                }
                return last;
            } catch (FluentExecutionStopped e) {
                lastRE = e;
                goAgain = retry(e, start, retries);
            } catch (RuntimeException e) {
                lastRE = e;
                goAgain = retry(e, start, retries);
            } catch (AssertionError e) {
                lastAE = e;
                goAgain = retry(e, start, retries);
            }
            if (goAgain) {
                try {
                    Thread.sleep(millisToSleep);
                } catch (InterruptedException e) {
                }
                retries++;
            }
        }
        if (lastRE != null) {
            if (lastRE instanceof FluentExecutionStopped && retries > 0) {
                ((FluentExecutionStopped) lastRE).setRetries(retries).setDuration(System.currentTimeMillis() - start);
            }
            throw lastRE;
        } else {
            throw lastAE;
        }
    }

    private boolean retry(Throwable e, long start, int retries) {
        boolean shouldRetry = retryStrategies.length > 0;
        for (RetryStrategy retryStrategy : retryStrategies) {
            shouldRetry = shouldRetry & retryStrategy.shouldRetry(e, start, retries);
        }
        return shouldRetry;
    }
}