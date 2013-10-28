
CMD=	ant
SRCDIR=	src/jp/gr/java_conf/neko_daisuki/android
PKG=	jp.gr.java_conf.neko_daisuki.android

all: apk

apk:
	@$(CMD)

release:
	@$(CMD) release

icon:
	@$(CMD) icon

clean:
	@$(CMD) clean

doc:
	@cd doc && $(MAKE)

prepare:
	@mkdir -p $(SRCDIR)/nexec/client
	@if [ ! -e $(PKG).animator ]; then				\
		ln -s $(SRCDIR)/animator $(PKG).animator;		\
	fi
	@if [ ! -e $(PKG).nexec.client ]; then				\
		ln -s $(SRCDIR)/nexec/client $(PKG).nexec.client;	\
	fi

.PHONY: doc icon
