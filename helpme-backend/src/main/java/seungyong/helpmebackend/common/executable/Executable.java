package seungyong.helpmebackend.common.executable;

public interface Executable<T, U> {
    U execute(T request);
}
