module com.checkorix {
    requires jdk.httpserver;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    opens com.checkorix.model to com.fasterxml.jackson.databind;
}