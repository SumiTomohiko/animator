
all: index.html icon.png

.SUFFIXES: .rst .html .svg .png

.rst.html:
	rst2html --stylesheet-path=default.css --link-stylesheet \
	--strip-comments < $< > $@

.svg.png:
	inkscape --export-png $@ $<

clean:
	rm -f *.html

icon.png: ../ic_launcher-web.png
	convert -thumbnail 256x ../ic_launcher-web.png icon.png
