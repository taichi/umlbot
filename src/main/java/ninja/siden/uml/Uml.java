package ninja.siden.uml;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;
import ninja.siden.App;
import ninja.siden.Renderer;
import ninja.siden.Request;
import ninja.siden.Response;
import ninja.siden.util.Loggers;

/**
 * @author taichi
 */
public class Uml {

	static final Logger LOG = Loggers.from(Uml.class);

	static final String ignored = "{\"text\":\"\"}";

	final String host;
	final String token;

	Uml(App app, String host, String token) {
		this.host = host;
		this.token = token;

		app.get("/:encoded", this::imgs);
		app.get("/", (req, res) -> "I'm running!! yey!");
		app.post("/", this::outgoing).type("application/json");
	}

	Object outgoing(Request request, Response response) throws Exception {
		LOG.info(request::toString);

		if (request.form("token").filter(token::equals).isPresent() == false) {
			return ignored;
		}

		String tw = request.form("trigger_word").orElse("");
		String txt = request.form("text").map(this::unescape).orElse("");
		if (txt.length() < tw.length()) {
			return ignored;
		}

		String content = txt.substring(tw.length()).trim();
		String end = "@enduml";
		if (content.endsWith(end)) {
			content = content.substring(0, content.length() - end.length());
		}
		if (5000 < content.length()) {
			StringBuilder stb = new StringBuilder();
			stb.append("{\"text\":\"");
			stb.append("very large content has come. i cant process huge content.");
			stb.append("\"}");
			return stb;
		}

		StringBuilder stb = new StringBuilder(100);
		stb.append("{\"text\":\"");
		stb.append(host);
		stb.append('/');
		stb.append(transcoder().encode(content));
		stb.append("\"}");
		return stb;
	}
	
	String unescape(String txt) {
		// see. https://api.slack.com/docs/formatting
		return txt.replace("&amp", "&").replace("&lt;", "<").replace("&gt;", ">");
	}

	static Transcoder transcoder() {
		return TranscoderUtil.getDefaultTranscoder();
	}

	Object imgs(Request request, Response response) throws Exception {
		Optional<String> dec = request.params("encoded").map(v -> {
			try {
				return transcoder().decode(v);
			} catch (Exception e) {
				LOG.log(Level.SEVERE, e.getMessage(), e);
				return null;
			}
		});
		if (dec.isPresent()) {
			response.type("image/png");
			SourceStringReader ssr = new SourceStringReader(dec.get());
			return response.render(ssr, Renderer.ofStream((m, os) -> ssr
					.generateImage(os, new FileFormatOption(FileFormat.PNG,
							false))));
		}
		return 404;
	}

	public static void main(String[] args) {
		String host = System.getenv("HOST");
		if (host == null || host.isEmpty()) {
			LOG.severe("HOST is not defined.");
			return;
		}
		try {
			URL url = new URL(host);
			if (url.getProtocol().startsWith("http") == false) {
				LOG.severe("HOST protocol must be http");
				return;
			}
		} catch (IOException e) {
			LOG.severe("HOST is not valid url.");
			return;
		}

		String token = System.getenv("TOKEN");
		if (token == null || token.isEmpty()) {
			LOG.severe("TOKEN is not defined.");
			return;
		}

		String port = System.getenv("PORT");
		int p = 8080;
		if (port != null && Pattern.matches("\\d{1,5}", port)) {
			int i = Integer.parseInt(port);
			if (0 < i && i < 65536) {
				p = i;
			}
		}
		LOG.info(String.format("PORT:%s using:%d", port, p));

		App app = new App();
		new Uml(app, host, token);
		Runtime.getRuntime().addShutdownHook(
				new Thread(app.listen("0.0.0.0", p)::stop));
	}
}
