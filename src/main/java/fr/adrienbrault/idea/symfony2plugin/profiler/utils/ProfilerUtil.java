package fr.adrienbrault.idea.symfony2plugin.profiler.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import consulo.application.ApplicationManager;
import consulo.html.language.HTMLLanguage;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlTagValue;
import consulo.xml.psi.impl.source.html.HtmlFileImpl;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.HttpDefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.HttpProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ProfilerUtil {
    private static final long MAX_PROFILER_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PROFILER_CONTENT_CHARS = 5 * 1024 * 1024;
    private static final int MAX_PROFILER_HTTP_CHARS = 2 * 1024 * 1024;
    private static final int PROFILER_CONNECT_TIMEOUT_MS = 3000;
    private static final int PROFILER_READ_TIMEOUT_MS = 5000;

    /**
     * Cache for url content
     */
    private static final Cache<String, String> REQUEST_CACHE = CacheBuilder.newBuilder()
        .maximumSize(50)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private static final Cache<String, ProfilerRequestInterface> PROFILER_REQUEST_CACHE = CacheBuilder.newBuilder()
        .maximumSize(15)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    /**
     * Extract "table.search-results tbody tr td"
     * We dont have complete xpath with html support inside so reuse internal html parser
     */
    public static Collection<ProfilerRequestInterface> createRequestsFromIndexHtml(Project project, String html, String baseUrl) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText("foo.bar", HTMLLanguage.INSTANCE, html);

        final XmlTag[] result = new XmlTag[1];

        // find table
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(psiElement instanceof XmlTag && "table".equals(((XmlTag) psiElement).getName()) && "search-results".equals(((XmlTag) psiElement).getAttributeValue("id"))) {
                result[0] = (XmlTag) psiElement;
                return false;
            }
            return true;
        });

        if(result[0] == null) {
            return Collections.emptyList();
        }

        // find table to be our keys for Map
        XmlTag thead = result[0].findFirstSubTag("thead");
        if(thead == null) {
            return Collections.emptyList();
        }

        XmlTag tr1 = thead.findFirstSubTag("tr");
        if(tr1 == null) {
            return Collections.emptyList();
        }

        List<String> header = new ArrayList<>();
        for (XmlTag th : tr1.findSubTags("th")) {
            header.add(StringUtils.trim(stripHtmlTags(th.getValue().getText())).toLowerCase());
        }

        // we need at least this fields
        if(!new HashSet<>(header).containsAll(Arrays.asList("token", "url"))) {
            return Collections.emptyList();
        }

        XmlTag tbody = result[0].findFirstSubTag("tbody");
        if(tbody == null) {
            return Collections.emptyList();
        }

        List<ProfilerRequestInterface> requests = new ArrayList<>();
        for (XmlTag tr : tbody.findSubTags("tr")) {

            // secure limit
            if(requests.size() >= 10) {
                break;
            }

            // "td" elements dont match header "th"
            XmlTag[] findSubTags = tr.findSubTags("td");
            if(findSubTags.length < header.size()) {
                continue;
            }

            // build row map with header keys
            Map<String, Pair<XmlTag, String>> row = new HashMap<>();
            for (int i = 0; i < findSubTags.length; i++) {
                row.put(header.get(i), Pair.create(
                    findSubTags[i],
                    StringUtils.trim(stripHtmlTags(findSubTags[i].getText().replaceAll("\\n", " "))).replace("\\s+", " ")
                ));
            }

            // extract token link to be our linked profiler url
            String profilerUrl = null;
            XmlTag tokenLink = row.get("token").getFirst().findFirstSubTag("a");
            if(tokenLink != null) {
                String href = tokenLink.getAttributeValue("href");
                if(StringUtils.isNotBlank(href)) {
                    profilerUrl = getProfilerAbsoluteUrl(baseUrl, href);
                }
            }

            // extract status code
            int statusCode = 0;
            if(row.containsKey("status")) {
                try {
                    statusCode = Integer.parseInt(row.get("status").getSecond());
                } catch (NumberFormatException ignored) {
                }
            }

            requests.add(new HttpProfilerRequest(
                statusCode,
                row.get("token").getSecond(),
                profilerUrl,
                row.containsKey("method") ? row.get("method").getSecond() : "n/a",
                row.get("url").getSecond()
            ));
        }

        return requests;
    }
    private static String getProfilerAbsoluteUrl(String baseUrl, String href) {
        return StringUtils.stripEnd(baseUrl, "/") + href.substring(href.indexOf("/_profiler/"));
    }
    public static Collection<ProfilerRequestInterface> collectHttpDataForRequest(Project project, Collection<ProfilerRequestInterface> requests) {
        Collection<Callable<ProfilerRequestInterface>> callable = requests.stream().map(
            request -> new MyProfilerRequestDecoratedCollectorCallable(project, request)).collect(Collectors.toCollection(ArrayList::new)
        );

        return getProfilerRequestCollectorDecorated(callable, 10);
    }

    /**
     * "_controller" and "_route"
     * "/_profiler/242e61?panel=request"
     *
     * <tr>
     *  <th>_route</th>
     *  <td>foo_route</td>
     * </tr>
     */
    public static Map<String, String> getRequestAttributes(Project project, String html) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText("foo.bar", HTMLLanguage.INSTANCE, html);

        String[] keys = new String[] {"_controller", "_route"};

        Map<String, String> map = new HashMap<>();
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(!(psiElement instanceof XmlTag) || !"th".equals(((XmlTag) psiElement).getName())) {
                return true;
            }

            XmlTagValue keyTag = ((XmlTag) psiElement).getValue();
            String key = StringUtils.trim(keyTag.getText());
            if(!ArrayUtils.contains(keys, key)) {
                return true;
            }

            XmlTag tdTag = PsiTreeUtil.getNextSiblingOfType(psiElement, XmlTag.class);
            if(tdTag == null || !"td".equals(tdTag.getName())) {
                return true;
            }

            XmlTagValue valueTag = tdTag.getValue();
            String value = valueTag.getText();
            if(StringUtils.isBlank(value)) {
                return true;
            }

            // Symfony 3.2 profiler debug? strip html
            map.put(key, stripHtmlTags(value));

            // exit if all item found
            return map.size() != keys.length;
        });

        return map;
    }

    /**
     * ["foo/foo.html.twig": 1]
     *
     * <tr>
     *  <td>@Twig/Exception/traces_text.html.twig</td>
     *  <td class="font-normal">1</td>
     * </tr>
     */
    public static Map<String, Integer> getRenderedElementTwigTemplates(Project project, String html) {
        HtmlFileImpl htmlFile = (HtmlFileImpl) PsiFileFactory.getInstance(project).createFileFromText("foo.bar", HTMLLanguage.INSTANCE, html);

        final XmlTag[] xmlTag = new XmlTag[1];
        PsiTreeUtil.processElements(htmlFile, psiElement -> {
            if(!(psiElement instanceof XmlTag) || !"h2".equals(((XmlTag) psiElement).getName())) {
                return true;
            }

            XmlTagValue keyTag = ((XmlTag) psiElement).getValue();
            String contents = StringUtils.trim(keyTag.getText());
            if(!"Rendered Templates".equalsIgnoreCase(contents)) {
                return true;
            }

            xmlTag[0] = (XmlTag) psiElement;

            return true;
        });

        if(xmlTag[0] == null) {
            return Collections.emptyMap();
        }

        XmlTag tableTag = PsiTreeUtil.getNextSiblingOfType(xmlTag[0], XmlTag.class);
        if(tableTag == null || !"table".equals(tableTag.getName())) {
            return Collections.emptyMap();
        }

        XmlTag tbody = tableTag.findFirstSubTag("tbody");
        if(tbody == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> templates = new LinkedHashMap<>();

        for (XmlTag tag : PsiTreeUtil.getChildrenOfTypeAsList(tbody, XmlTag.class)) {
            if(!"tr".equals(tag.getName())) {
                continue;
            }

            XmlTag[] tds = tag.findSubTags("td");
            if(tds.length < 2) {
                continue;
            }

            String template = stripHtmlTags(StringUtils.trim(tds[0].getValue().getText()));
            if(StringUtils.isBlank(template)) {
                continue;
            }

            int count;
            try {
                count = Integer.parseInt(stripHtmlTags(StringUtils.trim(tds[1].getValue().getText())));
            } catch (NumberFormatException e) {
                count = 0;
            }

            templates.put(template, count);
        }

        return templates;
    }
    public static List<String> getRenderedElementTwigTemplateNames(Project project, String html) {
        return new ArrayList<>(getRenderedElementTwigTemplates(project, html).keySet());
    }
    private static String stripHtmlTags(String text)
    {
        return text.replaceAll("<[^>]*>", "");
    }

    @Nullable
    public static String getProfilerUrlContent(String url) {
        URI uri = createHttpProfilerUri(url);
        if (uri == null) {
            return null;
        }

        URLConnection conn;
        try {
            conn = uri.toURL().openConnection();
            conn.setConnectTimeout(PROFILER_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(PROFILER_READ_TIMEOUT_MS);

            if (conn instanceof HttpURLConnection httpURLConnection) {
                httpURLConnection.setInstanceFollowRedirects(false);
            }
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return readHttpLimited(reader);
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    public static String normalizeHttpProfilerBaseUrl(@Nullable String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        URI uri = createHttpProfilerUri(StringUtils.stripEnd(url, "/"));
        if (uri == null) {
            return null;
        }

        return StringUtils.stripEnd(uri.toString(), "/");
    }

    @Nullable
    private static URI createHttpProfilerUri(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }

            if (StringUtils.isBlank(uri.getHost()) || StringUtils.isNotBlank(uri.getUserInfo())) {
                return null;
            }

            return uri;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return null;
        }
    }

    private static class MyProfilerRequestDecoratedCollectorCallable implements Callable<ProfilerRequestInterface> {
        private final Project project;
        private final ProfilerRequestInterface request;
        private final String profilerUrl;

        MyProfilerRequestDecoratedCollectorCallable(Project project, ProfilerRequestInterface request) {
            this.project = project;
            this.request = request;
            this.profilerUrl = request.getProfilerUrl();
        }

        @Override
        public ProfilerRequestInterface call() throws Exception {
            ProfilerRequestInterface requestCache = PROFILER_REQUEST_CACHE.getIfPresent(profilerUrl);
            if(requestCache != null) {
                return requestCache;
            }

            Map<String, String> requestAttributes = new HashMap<>();
            List<String> renderedTemplates = getRenderedTemplates(requestAttributes);

            ProfilerRequestInterface httpProfilerRequest = new HttpProfilerRequest(
                request,
                new HttpDefaultDataCollector(requestAttributes, renderedTemplates)
            );

            PROFILER_REQUEST_CACHE.put(profilerUrl, httpProfilerRequest);

            return httpProfilerRequest;
        }
        private List<String> getRenderedTemplates(Map<String, String> requestAttributes) {
            String requestContent = getUrlContent(profilerUrl + "?panel=request");
            String twigContent = getUrlContent(profilerUrl + "?panel=twig");

            if(requestContent != null) {
                ApplicationManager.getApplication().runReadAction(() ->
                    requestAttributes.putAll(ProfilerUtil.getRequestAttributes(project, requestContent))
                );
            }

            if(twigContent != null) {
                List<String> templates = new ArrayList<>();
                ApplicationManager.getApplication().runReadAction(() -> {
                    templates.addAll(getRenderedElementTwigTemplateNames(project, twigContent));
                    if(!templates.isEmpty()) {
                        String entryView = templates.stream()
                            .filter(template -> !template.startsWith("@WebProfiler"))
                            .findFirst()
                            .orElse(templates.get(0));
                        requestAttributes.put("_template", entryView);
                    }
                });

                return templates;
            }

            return Collections.emptyList();
        }

        private String getUrlContent(String url) {
            String contents = REQUEST_CACHE.getIfPresent(url);

            if(contents == null) {
                contents = ProfilerUtil.getProfilerUrlContent(url);
                if (contents != null) {
                    REQUEST_CACHE.put(url, contents);
                }
            }

            return contents;
        }
    }

    /**
     * Decorated request model with loaded collector data
     * loads data on multiple thread to be as fast as possible
     */
    public static List<ProfilerRequestInterface> getProfilerRequestCollectorDecorated(Collection<Callable<ProfilerRequestInterface>> callable, int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<ProfilerRequestInterface>> futures;
        try {
            futures = executor.invokeAll(callable);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }

        List<ProfilerRequestInterface> requests = new ArrayList<>();
        for (Future<ProfilerRequestInterface> future : futures) {
            try {
                requests.add(future.get());
            } catch (ExecutionException | InterruptedException ignored) {
            }
        }

        executor.shutdown();

        return requests;
    }

    /**
     * Try to find a base url profiler relative url:
     *  "/foobar" =>  "http://127.0.0.1:8000/foobar"
     *
     * In local csv context we dont know path info. There is a way
     * to try extract it from serialized string but overhead
     *
     * Think of:
     * http://127.0.0.1/
     * http://127.0.0.1:8000/
     * https://127.0.0.1:8000/
     * https://127.0.0.1:8000/app_dev.php
     */
    @Nullable
    public static String getBaseProfilerUrlFromRequest(ProfilerRequestInterface request) {
        URL url = null;
        try {
            url = URI.create(request.getProfilerUrl()).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            try {
                url = URI.create(request.getUrl()).toURL();
            } catch (MalformedURLException | IllegalArgumentException ignored) {
            }
        }

        if (url == null) {
            return null;
        }

        String portValue = "";
        int port = url.getPort();
        if(port != -1 && port != 80) {
            portValue = ":" + port;
        }

        String pathSuffix = "";
        String urlPath = url.getPath();
        Matcher matcher = Pattern.compile(".*(/app_[\\w]{2,6}.php)/").matcher(urlPath);
        if(matcher.find()){
            pathSuffix = StringUtils.stripEnd(urlPath.substring(0, matcher.end()), "/");
        }

        return url.getProtocol() + "://" + url.getHost() + portValue + pathSuffix;
    }

    @Nullable
    public static String formatProfilerRow(ProfilerRequestInterface profilerRequest) {
        int statusCode = profilerRequest.getStatusCode();

        String path = profilerRequest.getUrl();

        try {
            URL url = URI.create(profilerRequest.getUrl()).toURL();
            path = url.getPath();

            Matcher matcher = Pattern.compile(".*(/app_[\\w]{2,6}.php)/").matcher(path);
            if(matcher.find()){
                path = "/" + path.substring(matcher.end());
            }

        } catch (MalformedURLException | IllegalArgumentException ignored) {
        }

        return String.format("(%s) %s", statusCode == 0 ? "n/a" : statusCode, StringUtils.abbreviate(path, 35));
    }

    @Nullable
    public static String getContentForFile(File file) {
        if (!file.isFile() || file.length() > MAX_PROFILER_FILE_BYTES) {
            return null;
        }

        boolean isGzipFile;

        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[3];

            int bytesRead = is.read(buffer);

            // check gzip header
            isGzipFile = bytesRead == 3
                && buffer[0] == 31
                && buffer[1] == -117
                && buffer[2] == 8;
        } catch (IOException e) {
            return null;
        }

        if (isGzipFile) {
            return getProfilerContentGzdecode(file);
        }

        return getContentForRaw(file);
    }

    @Nullable
    private static String getContentForRaw(File file) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            return readLimited(in);
        } catch (IOException ignored) {
            return null;
        }
    }

    @Nullable
    private static String getProfilerContentGzdecode(File file) {
        try (
            InputStream in = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8))
        ) {
            return readLimited(reader);
        } catch (IOException ignored) {
        }

        return null;
    }

    @Nullable
    private static String readLimited(BufferedReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            if (content.length() + line.length() > MAX_PROFILER_CONTENT_CHARS) {
                return null;
            }

            content.append(line);
        }

        return content.toString();
    }

    @Nullable
    private static String readHttpLimited(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[8192];
        int read;

        while ((read = reader.read(buffer)) != -1) {
            if (content.length() + read > MAX_PROFILER_HTTP_CHARS) {
                return null;
            }

            content.append(buffer, 0, read);
        }

        return content.toString();
    }
}
