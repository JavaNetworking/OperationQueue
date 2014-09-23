OperationQueue
==============

[![Build Status](https://travis-ci.org/JavaNetworking/OperationQueue.svg?branch=master)](https://travis-ci.org/JavaNetworking/OperationQueue) [![Platform](http://img.shields.io/badge/platform-java%7Candroid-lightgrey.svg)]()

Operation queue executes operations on threads

## Basic usage

### Build

#### Windows
```cmd
gradlew.bat build
```

#### Unix
```bash
./gradlew build
```

#### Examples

##### Basic example
```java
OperationQueue queue = new OperationQueue();

queue.addOperation(new BaseOperation() {
	@Override
	public synchronized void execute() {
		super.execute();

		// Thread executing code
	}
	@Override
	public synchronized void complete() {
		super.complete();

		// Notify someone when complete
	}
});
```

##### Subclassing example
```java
public class SubOperation extends BaseOperation {

	private List<String> stringsForWork;

	public SubOperation(List<String> strings) {
		this.stringsForWork = strings;
	}

	@Override
	public synchronized void execute() {
		super.execute();

		for (String string : stringsForWork) {
			// Work with strings
		}
	}

	@Override
	public synchronized void complete() {
		super.complete();

		// Notify someone
	}
}

// Example data for operations
ArrayList<String> strings = new ArrayList<String>();
strings.add("String1");
strings.add("String2");
strings.add("String3");

// Create operation instance
SubOperation operation = new SubOperation(strings);

// Add operation to queue
OperationQueue queue = new OperationQueue();

queue.addOperation(operation);
```

## License

OperationQueue is available under the MIT license. See the LICENSE for more info.
