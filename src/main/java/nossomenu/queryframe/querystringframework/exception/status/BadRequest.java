package nossomenu.queryframe.querystringframework.exception.status;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class BadRequest implements Serializable {

    private String message;

    public BadRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}
