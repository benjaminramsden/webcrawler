package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object targetObject;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Clock clock, Object targetObject, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.targetObject = targetObject;
    this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Instant startTime = Instant.now(clock);
    Instant endTime = null;
    Object methodReturn = null;

    try {
      methodReturn = method.invoke(targetObject, args);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } finally {
      if (method.isAnnotationPresent(Profiled.class)) {
        endTime = clock.instant();
        Duration duration = Duration.between(startTime, endTime);
        state.record(targetObject.getClass(), method, duration);
      }
    }
    return methodReturn;
  }
}
