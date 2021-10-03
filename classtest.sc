MyClass {
	var va, vb;

    *new { arg aa=1, bb=1;
        ^super.new.init(aa, bb)
    }

    init { arg aa, bb;
		"MyClass:".postln;
        postf("Init aa=%. bb = %\n", aa, bb);
		this.amethod();
    }

	amethod { arg aa, bb;
		var va;
		// postf("A Method using aa=%. bb = %\n", aa, bb);
		va = this.bmethod.value;
		postf("A method assigning va=% with another method\n", va);
	}

	bmethod {
		var vb;
		vb = 2;
	}
}