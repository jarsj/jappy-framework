import com.crispy.Log;

public class TestLogging {
    public static final Log LOG = Log.get("test");

    public static void main(String[] args) {
	LOG.info("HELLO THERE");
    }
}