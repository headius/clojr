require 'target/clojr-1.0.jar'
require 'lib/clojure-1.3.0.jar'
require 'jruby'

com.headius.clojr.ClojrLibrary.new.load(JRuby.runtime, false)

ref = Clojr::STM::Ref.new

p ref.deref
begin
  ref.set('foo')
rescue java.lang.IllegalStateException
  puts "no transaction!"
end

Clojr::STM.dosync do
  ref.set('foo')
end
p ref.deref

vector = Clojr::Persistent::Vector.new('a')
p vector[0]

vector = vector.cons('b')
p vector[1]

p vector[2, 'c']

vector2 = vector.assoc(1, 'b2')
p vector2[1]
p vector[1]
