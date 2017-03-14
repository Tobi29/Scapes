/*
 * Copyright 2012-2017 Tobi29
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tobi29.scapes.block

import org.tobi29.scapes.engine.utils.io.tag.*
import org.tobi29.scapes.engine.utils.math.min

class ItemStack(private var material: Material,
                private var data: Int,
                private var amount: Int = 1,
                private var metaData: MutableTagMap = MutableTagMap()) {
    private val registry = material.registry()

    constructor(item: ItemStack) : this(item.material, item.data, item.amount,
            item.metaData.toTag().toMutTag())

    constructor(registry: GameRegistry) : this(registry.air, 0, 0)

    constructor(material: Material,
                data: Int,
                metaData: MutableTagMap) : this(material, data, 1, metaData)

    fun amount(): Int {
        return amount
    }

    fun setAmount(amount: Int): ItemStack {
        this.amount = amount
        checkEmpty()
        return this
    }

    fun material(): Material {
        return material
    }

    fun setMaterial(material: Material): ItemStack {
        this.material = material
        if (amount == 0) {
            amount = 1
        }
        checkEmpty()
        return this
    }

    fun setMaterial(material: Material,
                    data: Int): ItemStack {
        this.material = material
        if (amount == 0) {
            amount = 1
        }
        this.data = data
        checkEmpty()
        return this
    }

    fun data(): Int {
        return data
    }

    fun setData(data: Int): ItemStack {
        this.data = data
        return this
    }

    fun set(item: ItemStack) {
        material = item.material
        data = item.data
        amount = item.amount
        metaData = item.metaData.toTag().toMutTag()
    }

    fun clear() {
        material = registry.air
        data = 0
        amount = 0
        metaData = MutableTagMap()
    }

    fun canStack(add: ItemStack): Int {
        return canStack(add, min(min(material.maxStackSize(this),
                add.material.maxStackSize(add)) - amount, add.amount))
    }

    fun canStack(add: ItemStack,
                 amount: Int): Int {
        if ((add.material != material ||
                add.data != data ||
                amount + this.amount > min(material.maxStackSize(this),
                        add.material.maxStackSize(add)) ||
                metaData != add.metaData) &&
                material != registry.air &&
                this.amount > 0) {
            return 0
        }
        return amount
    }

    fun canTake(take: ItemStack): Int {
        return canTake(take, take.amount)
    }

    fun canTake(take: ItemStack,
                amount: Int): Int {
        if (take.material !== material || take.data != data ||
                material === registry.air || this.amount <= 0) {
            return 0
        }
        return min(this.amount, amount)
    }

    fun write(map: ReadWriteTagMap) {
        map["Type"] = material.itemID()
        map["Data"] = data
        map["Amount"] = amount
        map["MetaData"] = metaData.toTag()
    }

    fun read(map: TagMap) {
        map["Type"]?.toInt()?.let {
            material = registry.get<Material>("Core", "Material")[it]
        }
        map["Data"]?.toInt()?.let { data = it }
        map["Amount"]?.toInt()?.let { amount = it }
        map["MetaData"]?.toMap()?.let { metaData = it.toMutTag() }
    }

    fun metaData(category: String) = metaData.mapMut(category)

    fun stack(add: ItemStack): Int {
        val amount = canStack(add)
        if (amount > 0) {
            return stack(add, amount)
        }
        return 0
    }

    fun stack(add: ItemStack,
              amount: Int): Int {
        if (amount < 0) {
            throw IllegalArgumentException("Negative amount: " + amount)
        }
        if (canStack(add, amount) < amount) {
            return 0
        }
        material = add.material
        data = add.data
        if (this.amount == 0) {
            metaData = add.metaData.toTag().toMutTag()
        }
        this.amount += amount
        add.amount -= amount
        add.checkEmpty()
        return amount
    }

    fun take(amount: Int = Int.MAX_VALUE): ItemStack? {
        var takeAmount = amount
        takeAmount = min(this.amount, takeAmount)
        if (material === registry.air || takeAmount <= 0) {
            return null
        }
        val give = ItemStack(this)
        give.setAmount(takeAmount)
        this.amount -= takeAmount
        checkEmpty()
        return give
    }

    fun take(take: ItemStack,
             amount: Int = take.amount): ItemStack? {
        if (take.material !== material || take.data != data ||
                material === registry.air || this.amount <= 0) {
            return null
        }
        val give = ItemStack(this)
        give.setAmount(min(this.amount, amount))
        this.amount -= give.amount
        checkEmpty()
        take.checkEmpty()
        return give
    }

    val isEmpty: Boolean
        get() = amount <= 0 || material === registry.air

    fun name(): String {
        return material.name(this)
    }

    private fun checkEmpty() {
        if (amount <= 0 || material === registry.air) {
            clear()
        }
    }
}
