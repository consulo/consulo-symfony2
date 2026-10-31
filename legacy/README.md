Upstream integrations kept out of the Consulo build until their Consulo plugins get the needed APIs:
terminal command specs (consulo-terminal), Markdown code fence languages (consulo-markdown), database actions
(consulo-database), DQL. Files are the unchanged upstream sources with their original paths.
- `src/main/resources/META-INF/plugin.xml`: the upstream descriptor (all registrations are annotations in the Consulo port)
- `src/test`: the upstream IntelliJ-framework tests and fixtures, to be ported to a headless integration test module (like consulo-php/php-it)
