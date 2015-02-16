Generate slides:
asciidoctor -T ./asciidoctor-reveal.js/templates/slim slides*.adoc
asciidoctor -T ./asciidoctor-reveal.js/templates/slim slides*.adoc && firefox slides*.html &

After generation actions:
TODO: how to do proper hook for asciidoc or how to create my own temlate?
sed -i 's|<body>|<body>\n           <img id="logo" src="reveal.js/lib/img/redhat-color-small.png" />|' slides*.html
sed -i 's|<code>|<code class="stretch">|' slides*.html

