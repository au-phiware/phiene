TESTS = test/*.coffee

clean:
	for js in $(find . -name '*.js')
	do
		if test -f ${js%.js}.coffee
		then
			rm $js
		fi
	done
test:
	@NODE_ENV=test ./node_modules/.bin/mocha \
			--require should \
			--reporter list \
			--slow 20 \
			--growl \
			$(TESTS)
.PHONY: test
