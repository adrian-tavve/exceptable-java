/** _Exceptable_ generated by red.enspi.exceptable.annotation.@ExceptableSignal */
package red.enspi.exceptable.signal;

public class RuntimeException
  extends java.lang.RuntimeException
  implements red.enspi.exceptable.Exceptable {

  private final Signal<?> signal;
  private final Signal.Context context;

  public RuntimeException() {
    this(Error.UnknownError, null, null);
  }

  public RuntimeException(Signal<?> signal, Signal.Context context, Throwable cause) {
    super(
      (signal != null) ? signal.message(context) : Error.UnknownError.message(context),
      cause);
    this.signal = (signal != null) ? signal : Error.UnknownError;
    this.context = context;
  }

  @Override
  public Signal.Context context() { return this.context; }

  @Override
  public Signal<?> signal() { return this.signal; }
}

