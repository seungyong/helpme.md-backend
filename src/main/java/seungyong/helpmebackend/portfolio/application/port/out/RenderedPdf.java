package seungyong.helpmebackend.portfolio.application.port.out;

public record RenderedPdf(byte[] bytes, int pageCount) {
}
