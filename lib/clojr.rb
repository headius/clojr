require 'java'
require 'jruby'
require 'jbundler'

begin
  Java::clojure.lang.Ref
rescue Exception
  fail "Clojure is not available"
end

require 'clojr_ext.jar'
com.headius.clojr.ClojrLibrary.new.load(JRuby.runtime, false)
