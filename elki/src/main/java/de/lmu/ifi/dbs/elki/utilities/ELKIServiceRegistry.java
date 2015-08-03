package de.lmu.ifi.dbs.elki.utilities;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Registry of available implementations in ELKI.
 *
 * @author Erich Schubert
 */
public class ELKIServiceRegistry {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ELKIServiceRegistry.class);

  /**
   * Class loader
   */
  private static final URLClassLoader CLASSLOADER = (URLClassLoader) ClassLoader.getSystemClassLoader();

  /**
   * Factory class postfix.
   */
  public static final String FACTORY_POSTFIX = "$Factory";

  /**
   * Singleton.
   */
  private static ELKIServiceRegistry registry;

  /**
   * Singleton constructor, use {@link #singleton()}.
   */
  private ELKIServiceRegistry() {
    // SINGLETON
  }

  /**
   * Get the singleon instance.
   *
   * @return Singleton instance.
   */
  public static ELKIServiceRegistry singleton() {
    if(registry == null) {
      registry = new ELKIServiceRegistry();
    }
    return registry;
  }

  /**
   * Registry data.
   */
  Map<Class<?>, Entry> data = new HashMap<Class<?>, Entry>();

  /**
   * Value to abuse for failures.
   */
  final static Class<?> FAILED_LOAD = Entry.class;

  /**
   * Entry in the service registry.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private static class Entry {
    /**
     * Reusable empty array.
     */
    private static final String[] EMPTY_ALIASES = new String[0];

    /**
     * Class names.
     */
    private String[] names = new String[3];

    /**
     * Loaded classes.
     */
    private Class<?>[] clazzes = new Class<?>[3];

    /**
     * Length.
     */
    private int len = 0;

    /**
     * Aliases hash map.
     */
    private String[] aliases = EMPTY_ALIASES;

    /**
     * Occupied entries in aliases.
     */
    private int aliaslen = 0;

    /**
     * Add a candidate.
     *
     * @param cname Candidate name
     */
    private void addName(String cname) {
      // Grow if needed:
      if(len == names.length) {
        final int nl = (len << 1) + 1;
        names = Arrays.copyOf(names, nl);
        clazzes = Arrays.copyOf(clazzes, nl);
      }
      names[len++] = cname;
    }

    /**
     * If a name has been resolved, add it.
     *
     * @param cname Name
     * @param c Resulting class
     */
    private void addHit(String cname, Class<?> c) {
      // Grow if needed:
      if(len == names.length) {
        final int nl = (len << 1) + 1;
        names = Arrays.copyOf(names, nl);
        clazzes = Arrays.copyOf(clazzes, nl);
      }
      names[len] = cname;
      clazzes[len] = c;
      ++len;
    }

    /**
     * Register a class alias.
     *
     * @param alias Alias name
     * @param cname Class name
     */
    private void addAlias(String alias, String cname) {
      if(aliases == EMPTY_ALIASES) {
        aliases = new String[6];
      }
      if(aliaslen == aliases.length) {
        aliases = Arrays.copyOf(aliases, aliaslen << 1);
      }
      aliases[aliaslen++] = alias;
      aliases[aliaslen++] = cname;
    }
  }

  /**
   * Register a class with the registry.
   *
   * @param parent Parent class
   * @param cname Class name
   */
  protected void register(Class<?> parent, String cname) {
    Entry e = data.get(parent);
    if(e == null) {
      data.put(parent, e = new Entry());
    }
    e.addName(cname);
  }

  /**
   * Register a class in the registry.
   *
   * Careful: do not use this from your code before first making sure this has
   * been fully initialized. Otherwise, other implementations will not be found.
   * Therefore, avoid calling this from your own Java code!
   *
   * @param parent Class
   * @param clazz Implementation
   */
  protected void register(Class<?> parent, Class<?> clazz) {
    Entry e = data.get(parent);
    if(e == null) {
      data.put(parent, e = new Entry());
    }
    final String cname = clazz.getCanonicalName();
    e.addHit(cname, clazz);
    if(clazz.isAnnotationPresent(Alias.class)) {
      Alias aliases = clazz.getAnnotation(Alias.class);
      for(String alias : aliases.value()) {
        e.addAlias(alias, cname);
      }
    }
  }

  /**
   * Register a class alias with the registry.
   *
   * @param parent Parent class
   * @param alias Alias name
   * @param cname Class name
   */
  protected void registerAlias(Class<?> parent, String alias, String cname) {
    Entry e = data.get(parent);
    assert(e != null);
    e.addAlias(alias, cname);
  }

  /**
   * Find an implementation of the given interface / super class, given a
   * relative class name or alias name.
   *
   * @param restrictionClass Restriction class
   * @param value Class name, relative class name, or nickname.
   * @return Class found or {@code null}
   */
  public <C> Class<? extends C> findImplementation(Class<? super C> restrictionClass, String value) {
    Entry e = data.get(restrictionClass);
    int pos = -1;
    Class<?> clazz = null;
    // First, try the lookup cache:
    if(e != null) {
      for(pos = 0; pos < e.len; pos++) {
        if(e.names[pos].equals(value)) {
          break;
        }
      }
      if(pos < e.len) {
        clazz = e.clazzes[pos];
      }
      else {
        pos = -1;
      }
    }
    else {
      if(LOG.isDebuggingFinest()) {
        LOG.debugFinest("Finding implementations for unregistered type: " + restrictionClass.getName() + " " + value);
      }
    }
    // Next, try alternative versions:
    if(clazz == null) {
      clazz = tryLoadClass(value + FACTORY_POSTFIX);
      if(clazz == null) {
        clazz = tryLoadClass(value);
      }
      if(clazz == null) {
        clazz = tryLoadClass(restrictionClass.getPackage().getName() + "." + value + FACTORY_POSTFIX);
        if(clazz == null) {
          clazz = tryLoadClass(restrictionClass.getPackage().getName() + "." + value);
        }
      }
    }
    // Last, try aliases:
    if(clazz == null && e != null && e.aliaslen > 0) {
      for(int i = 0; i < e.aliaslen; i++) {
        if(e.aliases[i++].equalsIgnoreCase(value)) {
          clazz = findImplementation(restrictionClass, e.aliases[i]);
          break;
        }
      }
    }
    if(clazz == null) {
      return null;
    }

    if(!restrictionClass.isAssignableFrom(clazz)) {
      LOG.warning("Invalid entry in service file for class " + restrictionClass.getName() + ": " + value);
      clazz = FAILED_LOAD;
    }
    if(e != null) {
      if(pos < 0) {
        e.addHit(value, clazz);
      }
      else {
        assert(e.names[pos].equalsIgnoreCase(value));
        e.clazzes[pos] = clazz;
      }
    }
    if(clazz == FAILED_LOAD) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Class<? extends C> ret = (Class<? extends C>) clazz.asSubclass(restrictionClass);
    return ret;
  }

  /**
   * Attempt to load a class
   *
   * @param value Class name to try.
   * @return Class, or {@code null}.
   */
  private static Class<?> tryLoadClass(String value) {
    try {
      return CLASSLOADER.loadClass(value);
    }
    catch(ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Find all implementations of the given interface / super class.
   *
   * @param restrictionClass Restriction class
   * @return Iterator of classes
   */
  public List<Class<?>> findAllImplementations(Class<?> restrictionClass) {
    Entry e = data.get(restrictionClass);
    if(e == null) {
      return Collections.emptyList();
    }
    ArrayList<Class<?>> ret = new ArrayList<>(e.len);
    for(int pos = 0; pos < e.len; pos++) {
      Class<?> c = e.clazzes[pos];
      if(c == null) {
        c = tryLoadClass(e.names[pos]);
        if(c == null) {
          LOG.warning("Failed to load class " + e.names[pos] + " for interface " + restrictionClass.getName());
          e.clazzes[pos] = FAILED_LOAD;
        }
        e.clazzes[pos] = c;
      }
      if(c == FAILED_LOAD) {
        continue;
      }
      // Linear scan, but cheap enough.
      if(!ret.contains(c)) {
        ret.add(c);
      }
    }
    return ret;
  }

  /**
   * Test if a registry entry has already been created.
   *
   * @param c Class
   * @return {@code true} if a registry entry has been created.
   */
  protected boolean contains(Class<?> c) {
    return data.containsKey(c);
  }
}