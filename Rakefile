# First, all basic dependencies.
# ##############################
errors = []
['java', 'ant', 'jbundler'].each do |lib|
  begin
    require lib
  rescue Exception => e
    errors << e
  end
end

if errors.size > 0
  puts "========================================================="
  puts "Can not continue. The following exceptions were reported."
  errors.each do |e|
    puts "*** #{e} ***"
  end
  fail "Aborting!"
end

directory "pkg/classes"
directory "lib"

desc "Clean up build artifacts"
task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/clojr_ext.jar"
end

desc "Compile the extension"
task :compile => ["pkg/classes"] do |t|
  ENV['JAVA_OPTS'] = "-Xlint:unchecked"
  ant.javac :srcdir => "src", :destdir => t.prerequisites.first,
    :debug => true,
    :includeantruntime => false, :classpath => $CLASSPATH.to_a.join(':')
end

desc "Build the jar"
task :jar => [:compile, "lib"] do
  ant.jar :basedir => "pkg/classes", :destfile => "lib/clojr_ext.jar", :includes => "**/*.class"
end
 
task :package => :jar

desc "Run the specs"
task :spec => :jar do
  ruby "-S", "spec", "spec"
end
