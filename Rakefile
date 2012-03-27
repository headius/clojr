# First, all basic dependencies.
# ##############################
errors = []
['java', 'ant', 'mvn:org.clojure:clojure'].each do |lib|
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

##################################################################
# We copy gemified-mavens' jars here so that they are not required
# to be pushed into our repository.
LIBCACHEDIR = ".libcache"

directory "pkg/classes"
directory LIBCACHEDIR
directory "lib"

desc "Clean up build artifacts"
task :clean do
  rm_rf "pkg/classes"
  rm_rf "lib/clojr_ext.jar"
  rm_rf LIBCACHEDIR
end

task :libcache => LIBCACHEDIR do
  $CLASSPATH.each do |c|
    if c =~ /mvn:/
      jar = c.split('file:')[1]
      # This is a gemified maven artifact - pull in its jar into LIBCACHEDIR
      FileUtils.cp jar, LIBCACHEDIR
    end
  end
end

desc "Compile the extension"
task :compile => ["pkg/classes", :libcache] do |t|
  # Set up the classpath to include jars from our maven-gems copied to LIBCACHEDIR
  cpstring = ""
  Dir["#{File.dirname(__FILE__)}/#{LIBCACHEDIR}/*.jar"].each do |jar|
    cpstring = "#{jar}:#{cpstring}"
  end
  ant.javac :srcdir => "src", :destdir => t.prerequisites.first,
    :source => "1.5", :target => "1.5", :debug => true,
    :includeantruntime => false,
    :classpath => "${java.class.path}:${sun.boot.class.path}:#{cpstring}"
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
