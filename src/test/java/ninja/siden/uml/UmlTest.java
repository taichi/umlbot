package ninja.siden.uml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import mockit.Mock;
import mockit.MockUp;
import net.sourceforge.plantuml.code.Transcoder;
import ninja.siden.App;
import ninja.siden.Renderer;
import ninja.siden.Request;
import ninja.siden.Response;
import ninja.siden.util.Loggers;

import org.boon.json.JsonFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author taichi
 */
public class UmlTest {

	App.Stoppable stopper;
	Uml target;

	static final String HOST = "http://example.com";
	static final String TOKEN = "xxxxxxxxxx";

	@BeforeClass
	public static void beforeClass() {
		Loggers.setFinest(Uml.LOG);
	}

	@Before
	public void setUp() {
		App app = new App();
		this.target = new Uml(app, HOST, TOKEN);
		this.stopper = app.listen();
	}

	@After
	public void tearDown() {
		this.stopper.stop();
	}

	@Test
	public void outgoing() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("token", TOKEN);
		map.put("trigger_word", "@startuml");
		String content = "hogehoge";
		map.put("text", "@startuml\n" + content + "\n@enduml");

		Request request = new MockUp<Request>() {
			@Mock
			Optional<String> form(String key) {
				return Optional.ofNullable(map.get(key));
			}
		}.getMockInstance();
		Response response = new MockUp<Response>() {
		}.getMockInstance();
		Object obj = this.target.outgoing(request, response);
		assertNotNull(obj);
		System.out.println(obj);
		Msg msg = JsonFactory.fromJson(obj.toString(), Msg.class);
		assertNotNull(msg.text);
		assertTrue(msg.text.startsWith(HOST));

		Transcoder t = Uml.transcoder();
		String enc = t.encode(content);
		assertTrue(msg.text.endsWith(enc));
	}

	static class Msg {
		String text;
	}

	@Test
	public void imgs() throws Exception {
		String content = "Bob->Alice : hello";
		String encoded = Uml.transcoder().encode(content);
		Request request = new MockUp<Request>() {
			@Mock
			Optional<String> params(String key) {
				return Optional.of(encoded);
			}
		}.getMockInstance();
		Response response = new MockUp<Response>() {
			@Mock(invocations = 1)
			Void render(Object model, Renderer renderer) throws Exception {
				return null;
			}
		}.getMockInstance();
		Object result = this.target.imgs(request, response);
		assertNull(result);
	}
}
