package ninja.siden.uml;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;
import ninja.siden.App;
import ninja.siden.Renderer;
import ninja.siden.Request;
import ninja.siden.Response;
import ninja.siden.util.Trial;

/**
 * @author taichi
 */
public class Uml {

	static final Logger LOG = Logger.getLogger(Uml.class);

	static final String ignored = "{\"text\":\"\"}";

	final String url;
	final Set<String> tokens;

	Uml(App app, String url, Set<String> tokens) {
		this.url = url;
		this.tokens = tokens;
		app.get("/favicon.ico", (req, res) -> Uml.class.getClassLoader().getResource("favicon.ico"));
		app.get("/:encoded", this::imgs);
		app.get("/", (req, res) -> "I'm running!! yey!");
		app.post("/", this::outgoing).type("application/json");
	}

	Object outgoing(Request request, Response response) throws Exception {
		LOG.debug(request);

		if (request.form("token").filter(tokens::contains).isPresent() == false) {
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
		stb.append(url);
		stb.append('/');
		stb.append(transcoder().encode(content));
		stb.append("\"}");
		return stb;
	}

	String unescape(String txt) {
		// https://api.slack.com/docs/formatting
		return txt.replace("&amp", "&").replace("&lt;", "<").replace("&gt;", ">");
	}

	static Transcoder transcoder() {
		return TranscoderUtil.getDefaultTranscoder();
	}

	Object imgs(Request request, Response response) throws Exception {
		return request.params("encoded").map(Trial.of(transcoder()::decode))
				.map(t -> t.either(SourceStringReader::new, ex -> {
					LOG.fatal(ex.getMessage(), ex);
					return null;
				}))
				.map(v -> response.type("image/png").render(v,
						Renderer.ofStream((m, os) -> m.generateImage(os, new FileFormatOption(FileFormat.PNG, false)))))
				.orElse(404);
	}

	public static void main(String[] args) {
		// https://devcenter.heroku.com/articles/dynos#local-environment-variables
		LOG.info(System.getenv());

		String url = System.getenv("URL");
		if (url == null || url.isEmpty()) {
			LOG.fatal("URL is not defined.");
			return;
		}
		try {
			URL u = new URL(url);
			if (u.getProtocol().startsWith("http") == false) {
				LOG.fatal("URL protocol must be http");
				return;
			}
		} catch (IOException e) {
			LOG.fatal("URL is not valid.");
			return;
		}

		String token = System.getenv("TOKEN");
		if (token == null || token.isEmpty()) {
			LOG.fatal("TOKEN is not defined.");
			return;
		}
		Set<String> tokens = new HashSet<>(Arrays.asList(token.split(",")));

		String port = System.getenv("PORT");
		int p = 8080;
		if (port != null && Pattern.matches("\\d{1,5}", port)) {
			int i = Integer.parseInt(port);
			if (0 < i && i < 65536) {
				p = i;
			}
		}

		App app = new App() {
			@Override
			protected HttpHandler buildHandlers() {
				DirectBufferCache cache = new DirectBufferCache(1024, 10, 1024 * 1024 * 200);
				return new CacheHandler(cache, super.buildHandlers());
			}
		};
		new Uml(app, url, tokens);
		app.listen(p).addShutdownHook();
	}
}
