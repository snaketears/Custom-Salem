package haven;

import java.lang.reflect.*;
import java.io.*;
import java.net.URL;
import javax.jnlp.*;

public class JnlpCache implements ResCache {
    private PersistenceService back;
    private URL base;
    
    private JnlpCache(PersistenceService back, URL base) {
	this.back = back;
	this.base = base;
    }
    
    public static JnlpCache create() {
	try {
	    Class<? extends ServiceManager> cl = Class.forName("javax.jnlp.ServiceManager").asSubclass(ServiceManager.class);
	    Method m = cl.getMethod("lookup", String.class);
	    BasicService basic = (BasicService)m.invoke(null, "javax.jnlp.BasicService");
	    PersistenceService prs = (PersistenceService)m.invoke(null, "javax.jnlp.PersistenceService");
	    return(new JnlpCache(prs, basic.getCodeBase()));
	} catch(Exception e) {
	    return(null);
	}
    }
    
    private static String mangle(String nm) {
	StringBuilder buf = new StringBuilder();
	for(int i = 0; i < nm.length(); i++) {
	    char c = nm.charAt(i);
	    if(c == '/')
		buf.append("_");
	    else
		buf.append(c);
	}
	return(buf.toString());
    }

    private void put(URL loc, byte[] data) {
	FileContents file;
	try {
	    try {
		file = back.get(loc);
	    } catch(FileNotFoundException e) {
		back.create(loc, data.length);
		file = back.get(loc);
	    }
	    if(file.getMaxLength() < data.length) {
		if(file.setMaxLength(data.length) < data.length) {
		    back.delete(loc);
		    return;
		}
	    }
	    OutputStream s = file.getOutputStream(true);
	    try {
		s.write(data);
	    } finally {
		s.close();
	    }
	} catch(IOException e) {
	    return;
	} catch(Exception e) {
	    /* There seems to be a strange bug in NetX. */
	    return;
	}
    }

    public OutputStream store(final String name) throws IOException {
	OutputStream ret = new ByteArrayOutputStream() {
		public void close() {
		    byte[] res = toByteArray();
		    try {
			put(new URL(base, mangle(name)), res);
		    } catch(java.net.MalformedURLException e) {
			throw(new RuntimeException(e));
		    }
		}
	    };
	return(ret);
    }
    
    public InputStream fetch(String name) throws IOException {
	try {
	    URL loc = new URL(base, mangle(name));
	    FileContents file = back.get(loc);
	    InputStream in = file.getInputStream();
	    return(in);
	} catch(IOException e) {
	    throw(e);
	} catch(Exception e) {
	    /* There seems to be a weird bug in NetX */
	    throw((IOException)(new IOException("Virtual NetX IO exception").initCause(e)));
	}
    }
}