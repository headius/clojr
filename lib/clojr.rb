require 'java'
require 'jruby'

begin
  Java::clojure.lang.Ref
rescue Exception
  require 'mvn:org.clojure:clojure'
  begin
    Java::clojure.lang.Ref
  rescue Exception
    fail "Clojure is not available"
  end
end

require 'clojr_ext.jar'
com.headius.clojr.ClojrLibrary.new.load(JRuby.runtime, false)
