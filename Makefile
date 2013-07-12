
CMD = ant

all: apk

apk:
	@$(CMD)

icon:
	@$(CMD) icon

clean:
	@$(CMD) clean

doc:
	@cd doc && $(MAKE)

.PHONY: doc
