package com.buyeong.umji.api.operation.catalog.application.port.out

import com.buyeong.umji.api.operation.catalog.application.model.*
import java.util.UUID

interface OperationCatalogPort {
    fun categories():List<CategoryView>
    fun brands(page:Int,size:Int):List<BrandView>
    fun products(page:Int,size:Int):ProductPageView
    fun product(id:UUID):ProductView?
    fun createCategory(command:CategoryCommand):CatalogResource
    fun createBrand(command:BrandCommand):CatalogResource
    fun createProduct(command:ProductCommand):CatalogResource
    fun updateProduct(id:UUID,command:ProductCommand):CatalogResource?
    fun addImage(id:UUID,command:ImageCommand):CatalogResource?
    fun addOption(id:UUID,command:OptionCommand):CatalogResource?
    fun addSku(id:UUID,command:SkuCommand):CatalogResource?
    fun updateStatus(id:UUID,command:ProductStatusCommand):CatalogResource?
}
