package com.headius.clojr;

import clojure.lang.IFn;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.ISeq;
import clojure.lang.LockingTransaction;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentTreeMap;
import clojure.lang.PersistentVector;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Callable;

public class ClojrLibrary implements Library {
    private Ruby runtime;

    public void load(Ruby ruby, boolean wrap) throws IOException {
        runtime = ruby;

        RubyModule clojr = ruby.defineModule("Clojr");

        RubyModule stm = clojr.defineModuleUnder("STM");
        stm.defineAnnotatedMethods(STM.class);

        RubyClass ref = stm.defineClassUnder("Ref", ruby.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new Ref(ruby, rubyClass);
            }
        });
        ref.defineAnnotatedMethods(Ref.class);

        RubyModule persistent = clojr.defineModuleUnder("Persistent");
//        stm.defineAnnotatedMethods(Persistent.class);

        RubyClass collection = persistent.defineClassUnder("Collection", ruby.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        collection.defineAnnotatedMethods(Collection.class);

        RubyClass vector = persistent.defineClassUnder("Vector", ruby.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new Vector(ruby, rubyClass);
            }
        });
        vector.defineAnnotatedMethods(Vector.class);

        RubyClass map = persistent.defineClassUnder("Map", ruby.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        map.defineAnnotatedMethods(Map.class);
    }

    public static class STM {
        @JRubyMethod(module = true)
        public static IRubyObject dosync(final ThreadContext context, final IRubyObject self, final Block block) throws Exception {
            final Ruby ruby = context.runtime;

            return (IRubyObject) LockingTransaction.runInTransaction(new Callable() {
                public Object call() throws Exception {
                    // re-get transaction in case this gets run in different threads
                    return block.call(ruby.getCurrentContext());
                }
            });
        }
    }

    public class Ref extends RubyObject {
        private final clojure.lang.Ref ref;

        public Ref(Ruby ruby, RubyClass clazz) {
            super(ruby, clazz);

            ref = new clojure.lang.Ref(ruby.getNil());
        }

        @JRubyMethod
        public IRubyObject deref() {
            return (IRubyObject)ref.deref();
        }

        @JRubyMethod
        public IRubyObject set(IRubyObject obj) throws Exception {
            return (IRubyObject)ref.set(obj);
        }
    }

    public static class Collection<T extends IPersistentCollection> extends RubyObject {
        protected T collection;

        public Collection(Ruby runtime, RubyClass clazz) {
            super(runtime, clazz);
        }

        public Collection(Ruby runtime, RubyClass clazz, T collection) {
            super(runtime, clazz);
            this.collection = collection;
        }

        @JRubyMethod
        public IRubyObject count(ThreadContext context) {
            return context.runtime.newFixnum(collection.count());
        }
    }

    public static class Vector extends Collection<IPersistentVector> {
        public Vector(Ruby runtime, RubyClass clazz) {
            super(runtime, clazz);
        }

        public Vector(Ruby runtime, RubyClass clazz, IPersistentVector collection) {
            super(runtime, clazz, collection);
        }

        @JRubyMethod
        public IRubyObject initialize(ThreadContext context) {
            collection = PersistentVector.EMPTY;
            return context.nil;
        }

        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject initial) {
            collection = PersistentVector.create(initial);
            return context.nil;
        }

        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject initial0, IRubyObject initial1) {
            collection = PersistentVector.create(initial0, initial1);
            return context.nil;
        }

        @JRubyMethod
        public IRubyObject initialize(ThreadContext context, IRubyObject initial0, IRubyObject initial1, IRubyObject initial2) {
            collection = PersistentVector.create(initial0, initial1, initial2);
            return context.nil;
        }

        @JRubyMethod(rest = true)
        public IRubyObject initialize(ThreadContext context, IRubyObject[] initials) {
            collection = PersistentVector.create((Object) initials);
            return context.nil;
        }

        @JRubyMethod
        public IRubyObject cons(ThreadContext context, IRubyObject obj) {
            return new Vector(context.runtime, metaClass, collection.cons(obj));
        }

        @JRubyMethod
        public IRubyObject empty(ThreadContext context) {
            return new Vector(context.runtime, metaClass, PersistentVector.EMPTY);
        }
        
        @JRubyMethod(name = {"nth", "[]"})
        public IRubyObject nth(ThreadContext context, IRubyObject index) {
            return (IRubyObject)collection.nth((int)index.convertToInteger().getLongValue(), context.nil);
        }

        @JRubyMethod(name = {"nth", "[]"})
        public IRubyObject nth(IRubyObject index, IRubyObject notFound) {
            return (IRubyObject)collection.nth((int)index.convertToInteger().getLongValue(), notFound);
        }
        
        @JRubyMethod
        public IRubyObject assoc(ThreadContext context, IRubyObject index, IRubyObject value) {
            return new Vector(context.runtime, metaClass, collection.assocN((int)index.convertToInteger().getLongValue(), value));
        }

        @JRubyMethod
        public IRubyObject pop(ThreadContext context) {
            return new Vector(context.runtime, metaClass, (PersistentVector)collection.pop());
        }
    }
    
    public static class Map extends Collection<IPersistentMap> {
        public Map(Ruby runtime, RubyClass clazz) {
            super(runtime, clazz);
        }

        public Map(Ruby runtime, RubyClass clazz, IPersistentMap collection) {
            super(runtime, clazz, collection);
        }
        
        @JRubyMethod(meta = true)
        public static IRubyObject array(ThreadContext context, IRubyObject cls) {
            return new Map(context.runtime, (RubyClass)cls, PersistentArrayMap.EMPTY);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject array(ThreadContext context, IRubyObject cls, IRubyObject arg) {
            return new Map(context.runtime, (RubyClass)cls, populate(PersistentArrayMap.EMPTY, asHash(context, arg)));
        }

        @JRubyMethod(meta = true)
        public static IRubyObject hash(ThreadContext context, IRubyObject cls) {
            return new Map(context.runtime, (RubyClass)cls, PersistentHashMap.EMPTY);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject hash(ThreadContext context, IRubyObject cls, IRubyObject arg) {
            return new Map(context.runtime, (RubyClass)cls, populate(PersistentHashMap.EMPTY, asHash(context, arg)));
        }

        @JRubyMethod(meta = true)
        public static IRubyObject tree(ThreadContext context, IRubyObject cls) {
            return new Map(context.runtime, (RubyClass)cls, PersistentTreeMap.EMPTY);
        }

        @JRubyMethod(meta = true)
        public static IRubyObject tree(ThreadContext context, IRubyObject cls, IRubyObject arg) {
            return new Map(context.runtime, (RubyClass)cls, populate(PersistentTreeMap.EMPTY, asHash(context, arg)));
        }

        @JRubyMethod
        public IRubyObject assoc(ThreadContext context, IRubyObject key, IRubyObject value) {
            return new Map(context.runtime, metaClass, collection.assoc(key, value));
        }

        @JRubyMethod
        public IRubyObject without(ThreadContext context, IRubyObject key) {
            return new Map(context.runtime, metaClass, collection.without(key));
        }

        @JRubyMethod(name = "key?")
        public IRubyObject key_p(ThreadContext context, IRubyObject key) {
            return context.runtime.newBoolean(collection.containsKey(key));
        }
        
        @JRubyMethod(name = "[]")
        public IRubyObject at(ThreadContext context, IRubyObject key) {
            return (IRubyObject)collection.entryAt(key).val();
        }

        private static RubyHash asHash(ThreadContext context, IRubyObject arg) {
            if (!(arg instanceof RubyHash)) {
                throw context.runtime.newTypeError(arg, context.runtime.getHash());
            }

            return (RubyHash)arg;
        }

        private static IPersistentMap populate(IPersistentMap map, RubyHash hash) {
            for (RubyHash.RubyHashEntry entry : (Set<RubyHash.RubyHashEntry>)hash.directEntrySet()) {
                map = map.assoc(entry.getKey(), entry.getValue());
            }

            return map;
        }
    }

    private static class BlockFn implements IFn {
        private final Ruby runtime;
        private final Block block;

        public BlockFn(Ruby runtime, Block block) {
            this.runtime = runtime;
            this.block = block;
        }

        public Object invoke() {
            return block.call(runtime.getCurrentContext());
        }

        public Object invoke(Object o) {
            return block.call(runtime.getCurrentContext(), (IRubyObject)o);
        }

        public Object invoke(Object o, Object o1) {
            return block.call(runtime.getCurrentContext(), (IRubyObject)o, (IRubyObject)o1);
        }

        public Object invoke(Object o, Object o1, Object o2) {
            return block.call(runtime.getCurrentContext(), (IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2);
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14, (IRubyObject)o15});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14, (IRubyObject)o15, (IRubyObject)o16});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16, Object o17) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14, (IRubyObject)o15, (IRubyObject)o16, (IRubyObject)o17});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16, Object o17, Object o18) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14, (IRubyObject)o15, (IRubyObject)o16, (IRubyObject)o17, (IRubyObject)o18});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16, Object o17, Object o18, Object o19) {
            return block.call(runtime.getCurrentContext(), new IRubyObject[] {(IRubyObject)o, (IRubyObject)o1, (IRubyObject)o2, (IRubyObject)o3, (IRubyObject)o4, (IRubyObject)o5, (IRubyObject)o6, (IRubyObject)o7, (IRubyObject)o8, (IRubyObject)o9, (IRubyObject)o10, (IRubyObject)o11, (IRubyObject)o12, (IRubyObject)o13, (IRubyObject)o14, (IRubyObject)o15, (IRubyObject)o16, (IRubyObject)o17, (IRubyObject)o18, (IRubyObject)o19});
        }

        public Object invoke(Object o, Object o1, Object o2, Object o3, Object o4, Object o5, Object o6, Object o7, Object o8, Object o9, Object o10, Object o11, Object o12, Object o13, Object o14, Object o15, Object o16, Object o17, Object o18, Object o19, Object... objects) {
            IRubyObject[] args = new IRubyObject[19 + objects.length];
            args[0] = (IRubyObject)o;
            args[1] = (IRubyObject)o1;
            args[2] = (IRubyObject)o2;
            args[3] = (IRubyObject)o3;
            args[4] = (IRubyObject)o4;
            args[5] = (IRubyObject)o5;
            args[6] = (IRubyObject)o6;
            args[7] = (IRubyObject)o7;
            args[8] = (IRubyObject)o8;
            args[9] = (IRubyObject)o9;
            args[10] = (IRubyObject)o10;
            args[11] = (IRubyObject)o11;
            args[12] = (IRubyObject)o12;
            args[13] = (IRubyObject)o13;
            args[14] = (IRubyObject)o14;
            args[15] = (IRubyObject)o15;
            args[16] = (IRubyObject)o16;
            args[17] = (IRubyObject)o17;
            args[18] = (IRubyObject)o18;
            args[19] = (IRubyObject)o19;
            System.arraycopy(objects, 0, args, 20, objects.length);
            return block.call(runtime.getCurrentContext(), args);
        }

        public Object applyTo(ISeq iSeq) {
            while (iSeq != null) {
                block.call(runtime.getCurrentContext(), (IRubyObject)iSeq.first());
                iSeq = iSeq.next();
            }
            return runtime.getNil();
        }

        public Object call() throws Exception {
            return block.call(runtime.getCurrentContext());
        }

        public void run() {
            block.call(runtime.getCurrentContext());
        }
    }
}
