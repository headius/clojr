require 'target/clojr-1.0.jar'
require 'lib/clojure-1.3.0.jar'
require 'jruby'

com.headius.clojr.ClojrLibrary.new.load(JRuby.runtime, false)

# STM

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

# Vector

vector = Clojr::Persistent::Vector.new('a')
p vector[0]

vector = vector.cons('b')
p vector[1]

p vector[2, 'c']

vector2 = vector.assoc(1, 'b2')
p vector2[1]
p vector[1]

# Map

hmap = Clojr::Persistent::Map.hash
%w[foo bar baz].each do |key|
  hmap = hmap.assoc key, key.upcase
end

p hmap['bar']
p hmap.key? 'foo'

hmap2 = hmap.assoc('baz', 'WOO')
p hmap2['baz']
p hmap['baz']