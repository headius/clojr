# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{clojr}
  s.version = "0.0.1"
  s.authors = ["Charles Oliver Nutter", "Ravindra R. Jaju"]
  s.date = Time.now
  s.description = "Ease your Clojure integration into JRuby, and vice versa. Include this gem, and follow a few simple rules to enjoy the best of both worlds!"
  s.email = ["headius@headius.com"]
  s.files = Dir['{lib,examples,test}/**/*'] + Dir['{*.txt,*.gemspec,Rakefile}']
  s.homepage = "http://github.com/headius/clojr"
  s.require_paths = ["lib"]
  s.summary = "Ease your Clojure integration into JRuby!"
  s.test_files = Dir["spec/*_spec.rb"]
  s.platform = "java"
  s.add_dependency "mvn:org.clojure:clojure", "~> 1.4"
end
