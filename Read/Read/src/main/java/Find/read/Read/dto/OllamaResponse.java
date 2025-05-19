package Find.read.Read.dto;

public class OllamaResponse {
    private String response; // or use streaming=true and handle chunks
    private boolean done;

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

}
