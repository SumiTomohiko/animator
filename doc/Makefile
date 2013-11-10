
THUMBNAILS=	screenshot-thumb.png
CONVERT=	convert -thumbnail 640x

all: index.html icon.png $(THUMBNAILS)

.SUFFIXES: .rst .html .svg .png

.rst.html:
	rst2html --stylesheet-path=default.css --link-stylesheet \
	--strip-comments < $< > $@

.svg.png:
	inkscape --export-png $@ $<

clean:
	rm -f *.html $(THUMBNAILS)

icon.png: ../ic_launcher-web.png
	convert -thumbnail 256x ../ic_launcher-web.png icon.png

force_stop-thumb.png: force_stop.png
	$(CONVERT) force_stop.png force_stop-thumb.png

force_stop_dialog-thumb.png: force_stop_dialog.png
	$(CONVERT) force_stop_dialog.png force_stop_dialog-thumb.png

nexec_client-thumb.png: nexec_client.png
	$(CONVERT) nexec_client.png nexec_client-thumb.png

running_services-thumb.png: running_services.png
	$(CONVERT) running_services.png running_services-thumb.png

screenshot-thumb.png: screenshot.png
	$(CONVERT) screenshot.png screenshot-thumb.png
